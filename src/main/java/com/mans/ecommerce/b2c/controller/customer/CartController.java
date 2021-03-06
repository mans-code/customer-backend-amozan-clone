package com.mans.ecommerce.b2c.controller.customer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;

import com.mans.ecommerce.b2c.controller.utill.dto.ProductDto;
import com.mans.ecommerce.b2c.domain.entity.customer.Cart;
import com.mans.ecommerce.b2c.domain.entity.sharedSubEntity.ProductInfo;
import com.mans.ecommerce.b2c.domain.enums.CartAction;
import com.mans.ecommerce.b2c.domain.exception.MissingVariationIdException;
import com.mans.ecommerce.b2c.domain.exception.OutOfStockException;
import com.mans.ecommerce.b2c.domain.exception.PartialOutOfStockException;
import com.mans.ecommerce.b2c.domain.logic.CartLogic;
import com.mans.ecommerce.b2c.security.jwt.JWTToken;
import com.mans.ecommerce.b2c.service.CartService;
import com.mans.ecommerce.b2c.service.CheckoutService;
import com.mans.ecommerce.b2c.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@RestController
@RequestMapping("/carts/{cartId}")
@Tag(name = "cart api", description = "manipulating customers cart")
public class CartController
{

    private CartService cartService;

    private ProductService productService;

    private CartLogic cartLogic;

    private CheckoutService checkoutService;

    private final int ZERO = 0;

    CartController(
            CartService cartService,
            ProductService productService,
            CartLogic cartLogic,
            CheckoutService checkoutService)
    {
        this.cartService = cartService;
        this.productService = productService;
        this.cartLogic = cartLogic;
        this.checkoutService = checkoutService;
    }

    @GetMapping
    @Operation(description = "returns the customer cart or 404 not found if the cart does not exists")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = Cart.class))),
            @ApiResponse(responseCode = "404", description = "Cart with the given id not found")
    })
    public Mono<Cart> getCart(@PathVariable("cartId") @NotNull ObjectId cartId)
    {
        return cartService.findById(cartId);
    }

    @PatchMapping
    @Operation(description = "update the customer cart or 404 not found if the cart does not exists. "
                                  + "To update cart please look at ProductDto for more info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = Cart.class))),
            @ApiResponse(responseCode = "206", description = "This product has only @availableQuantity@ of these available", content = @Content(schema = @Schema(implementation = Cart.class))),
            @ApiResponse(responseCode = "404", description = "Cart with the given id not found"),
            @ApiResponse(responseCode = "404", description = "product is Out Of Stock, Note: only when adding a product to cart"),
            @ApiResponse(responseCode = "400", description = "variation id must not be empty. Note: only if the customer update or delete")
    })
    public Mono<Cart> update(@PathVariable("cartId") @NotNull ObjectId cartId, @RequestBody @Valid ProductDto dto)
    {
        CartAction action = dto.getCartAction();

        if ((action == CartAction.DELETE || action == CartAction.UPDATE)
                    && Objects.isNull(dto.getVariationId()))
        {
            return Mono.error(new MissingVariationIdException());
        }

        Mono<Cart> cart = cartService.findById(cartId);

        switch (action)
        {
        case UPDATE:
            return updateProductInCart(cart, dto).log("Update Cart");
        case DELETE:
            return removerProductFromCart(cart, dto).log("Delete Product in Cart");
        case RESET:
            return reset(cart).log("Reset Cart");
        default:
            return addProductToCart(cart, dto).log("add TO Cart");
        }
    }

    public Mono<Cart> reset(Mono<Cart> cartMono)
    {
        return cartMono.flatMap(cart -> {
            if (cart.isActive())
            {
                checkoutService.unlock(cart, cart.getProductInfos());
            }
            cartLogic.removeAllProducts(cart);
            return cartService.update(cart);
        });
    }

    private Mono<Cart> removerProductFromCart(Mono<Cart> cartMono, ProductDto dto)
    {

        return cartMono.flatMap(cart -> {
            ProductInfo cartProduct = cartLogic.removeProduct(cart, dto);
            if (cart.isActive())
            {
                checkoutService.unlock(cart, cartProduct);
            }
            return cartService.update(cart);
        });
    }

    private Mono<Cart> addProductToCart(Mono<Cart> cartMono, ProductDto dto)
    {

        Mono<ProductInfo> productInfoMono = productService.getProductInfo(dto);
        Mono<Tuple2<Cart, ProductInfo>> tuple2Mono = cartMono.zipWith(productInfoMono);
        Mono<Cart> savedCartMon = tuple2Mono.flatMap(tuple2 -> {
            Cart cart = tuple2.getT1();
            ProductInfo productInfo = tuple2.getT2();
            if (productInfo.getQuantity() == 0)
            {
                return Mono.error(new OutOfStockException());
            }
            return addProductToCart(cart, productInfo, dto.getQuantity());
        });
        return savedCartMon;
    }

    private Mono<Cart> addProductToCart(Cart cart, ProductInfo cartProduct, int requestedQty)
    {

        if (!cart.isActive())
        {
            cartLogic.addProduct(cart, cartProduct);
            Mono<Cart> updateCart = cartService.update(cart);
            int availableQty = cartProduct.getQuantity();

            if (availableQty < requestedQty)
            {
                return updateCart.flatMap(updated -> Mono.error(new PartialOutOfStockException(updated, availableQty)));
            }
            return updateCart;
        }

        boolean existsInCart = cartLogic.isInCart(cart, cartProduct);
        Mono<Integer> lockMon;

        if (existsInCart)
        {
            lockMon = checkoutService.partialLock(cart, cartProduct, cartProduct.getQuantity());
        }
        else
        {
            lockMon = checkoutService.lock(cart, cartProduct);
        }

        return lockMon.flatMap(locked -> addLockedProduct(cart, cartProduct, locked, requestedQty));
    }

    private Mono<Cart> addLockedProduct(
            Cart cart,
            ProductInfo cartProduct,
            Integer locked,
            int requestedQty)
    {
        if (locked == 0)
        {
            return Mono.error(new OutOfStockException());
        }

        cartProduct.setQuantity(locked);
        cartLogic.addProduct(cart, cartProduct);
        Mono<Cart> cartUpdateMono = cartService.update(cart);

        if (locked < requestedQty)
        {
            return cartUpdateMono.flatMap(updated -> Mono.error(new PartialOutOfStockException(updated, locked)));
        }

        return cartUpdateMono;
    }

    private Mono<Cart> updateProductInCart(Mono<Cart> cartMono, ProductDto dto)
    {

        if (dto.getQuantity() == ZERO)
        {
            return removerProductFromCart(cartMono, dto);
        }

        return cartMono.flatMap(cart -> {
            ProductInfo cartProduct = cartLogic.getProduct(cart, dto);
            int difference = getQuantityDifference(dto, cartProduct);
            int absDifference = Math.abs(difference);
            if (difference == ZERO)
            {
                return cartMono;
            }
            else if (difference < ZERO)
            {
                dto.setQuantity(absDifference);
                return addProductToCart(cartMono, dto);
            }
            else
            {
                return reduceQuantity(cart, cartProduct, difference);
            }
        });

    }

    private Mono<Cart> reduceQuantity(Cart cart, ProductInfo cartProduct, int deductedQuantity)
    {
        cartLogic.deductMoneyAndQuantity(cart, cartProduct, deductedQuantity);
        return cartService.update(cart).doOnSuccess(updated -> partialUnlock(updated, cartProduct, deductedQuantity));
    }

    private void partialUnlock(Cart updated, ProductInfo cartProduct, int deductedQuantity)
    {
        if (updated != null && updated.isActive())
        {
            checkoutService.partialUnlock(updated, cartProduct, deductedQuantity);
        }
    }

    private int getQuantityDifference(ProductDto dto, ProductInfo cartProduct)
    {
        int oldQuantity = cartProduct.getQuantity();
        int newQuantity = dto.getQuantity();
        int difference = oldQuantity - newQuantity;

        return difference;
    }
}
