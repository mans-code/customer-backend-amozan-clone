package com.mans.ecommerce.b2c.e2e;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.mans.ecommerce.b2c.controller.utill.dto.ProductDto;
import com.mans.ecommerce.b2c.domain.entity.customer.Cart;
import com.mans.ecommerce.b2c.domain.entity.sharedSubEntity.ProductInfo;
import com.mans.ecommerce.b2c.domain.exception.OutOfStockException;
import com.mans.ecommerce.b2c.domain.exception.PartialOutOfStockException;
import com.mans.ecommerce.b2c.e2e.utill.ProductLockingValidator;
import com.mans.ecommerce.b2c.repository.product.ProductRepository;
import com.mans.ecommerce.b2c.service.CheckoutService;
import com.mans.ecommerce.b2c.utills.Global;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class ActiveCartControllerIT
{
    private final String errorMessageTemplate = "%s product info with sku=%s \n variationId=%s \n qty=%s \n amount=%s";

    private ProductLockingValidator lockingValidator;

    private final String BASE_URL = "/carts/{cartId}";

    @SpyBean
    private CheckoutService checkoutService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void add_pass()
    {
        String cartId = "5eaa32339e58d82df43199ac";
        String sku = "mans-50";
        String cartAction = "add";
        int lock = 10;
        BigDecimal productAmount = new BigDecimal("11");
        BigDecimal cartPervAmount = new BigDecimal("650");
        BigDecimal cartExpectedAmount = productAmount
                                                .multiply(BigDecimal.valueOf(lock))
                                                .add(cartPervAmount);

        ProductDto dto = getProductDto(sku, cartAction, lock);

        Map<String, Integer> productExpectation = ImmutableMap.<String, Integer>builder().put(sku, 520).build();
        Map<String, Integer> resExpectation = ImmutableMap.<String, Integer>builder().put(sku, lock).build();

        callApiAndVerifyPassing(cartId, productAmount, cartExpectedAmount, dto, productExpectation, resExpectation,
                                true, true);
    }

    @Test
    public void updateAdd_pass()
    {
        String cartId = "5eaa32339e58d82df43199b7";
        String sku = "mans-32";
        String cartAction = "update";
        int oldLock = 2;
        int lock = 10;
        BigDecimal productAmount = new BigDecimal("21");
        BigDecimal cartPervAmount = new BigDecimal("296");
        BigDecimal cartExpectedAmount = productAmount
                                                .multiply(BigDecimal.valueOf(lock - oldLock))
                                                .add(cartPervAmount);

        ProductDto dto = getProductDto(sku, cartAction, lock);

        Map<String, Integer> productExpectation = ImmutableMap.<String, Integer>builder().put(sku, 169).build();
        Map<String, Integer> resExpectation = ImmutableMap.<String, Integer>builder().put(sku, lock).build();

        callApiAndVerifyPassing(cartId, productAmount, cartExpectedAmount, dto, productExpectation, resExpectation,
                                true, true);
    }

    @Test
    public void updateReduce_pass()
    {
        String cartId = "5eaa32339e58d82df43199c1";
        String sku = "mans-7";
        String cartAction = "update";
        int oldLockQty = 5;
        int lock = 2;
        BigDecimal productAmount = new BigDecimal("33");
        BigDecimal cartPervAmount = new BigDecimal("195");
        BigDecimal cartExpectedAmount = cartPervAmount.subtract(
                productAmount.multiply(BigDecimal.valueOf(oldLockQty - lock)));

        ProductDto dto = getProductDto(sku, cartAction, lock);

        Map<String, Integer> productExpectation = ImmutableMap.<String, Integer>builder().put(sku, 922).build();
        Map<String, Integer> resExpectation = ImmutableMap.<String, Integer>builder().put(sku, lock).build();

        callApiAndVerifyPassing(cartId, productAmount, cartExpectedAmount, dto, productExpectation, resExpectation,
                                true, true);
    }

    @Test
    public void updateRemove_pass()
    {
        String cartId = "5eaa32339e58d82df43199bf";
        String sku = "mans-25";
        String cartAction = "update";

        BigDecimal cartExpectedAmount = new BigDecimal(0);
        BigDecimal productAmount = new BigDecimal("14");
        ProductDto dto = getProductDto(sku, cartAction, 0);

        Map<String, Integer> productExpectation = ImmutableMap.<String, Integer>builder().put(sku, 578).build();
        Map<String, Integer> resExpectation = ImmutableMap.<String, Integer>builder().put(sku, 5).build();

        callApiAndVerifyPassing(cartId, productAmount, cartExpectedAmount, dto, productExpectation, resExpectation,
                                false, false);
    }

    @Test
    public void remove_pass()
    {
        String cartId = "5eaa32339e58d82df43199bc";
        String sku = "mans-49";
        String cartAction = "delete";

        BigDecimal cartExpectedAmount = new BigDecimal(0);
        BigDecimal productAmount = new BigDecimal("37");
        ProductDto dto = getProductDto(sku, cartAction, 0);

        Map<String, Integer> productExpectation = ImmutableMap.<String, Integer>builder().put(sku, 162).build();
        Map<String, Integer> resExpectation = ImmutableMap.<String, Integer>builder().put(sku, 2).build();

        callApiAndVerifyPassing(cartId, productAmount, cartExpectedAmount, dto, productExpectation, resExpectation,
                                false, false);
    }

    @Test
    public void add_pass_PartialOutOfStock()
    {
        String cartId = "5eaa32339e58d82df43199c3";
        String sku = "mans-33";
        String cartAction = "add";

        BigDecimal productAmount = new BigDecimal("18");
        BigDecimal cartExpectedAmount = new BigDecimal("888");
        ProductDto dto = getProductDto(sku, cartAction, 10);

        Map<String, Integer> productExpectation = ImmutableMap.<String, Integer>builder().put(sku, 0).build();
        Map<String, Integer> resExpectation = ImmutableMap.<String, Integer>builder().put(sku, 5).build();

        callApiAndVerifyPartialOutOfStock(cartId, productAmount, cartExpectedAmount, dto, productExpectation,
                                          resExpectation);
    }

    @Test
    public void update_pass_PartialOutOfStock()
    {
        String cartId = "5eaa32339e58d82df4319997";
        String sku = "mans-34";
        String cartAction = "update";

        BigDecimal cartExpectedAmount = new BigDecimal("510");
        BigDecimal productAmount = new BigDecimal("14");
        ProductDto dto = getProductDto(sku, cartAction, 10);

        Map<String, Integer> productExpectation = ImmutableMap.<String, Integer>builder().put(sku, 0).build();
        Map<String, Integer> resExpectation = ImmutableMap.<String, Integer>builder().put(sku, 6).build();

        callApiAndVerifyPartialOutOfStock(cartId, productAmount, cartExpectedAmount, dto, productExpectation,
                                          resExpectation);
    }

    @Test
    public void add_fail_outOfStock()
    {
        String cartId = "5eaa32339e58d82df431999a";
        outOfStock(cartId, "add");
        verify(checkoutService, times(0)).lock(any(Cart.class), any(ProductInfo.class));
    }

    @Test
    public void update_fail_outOfStock()
    {
        String cartId = "5eaa32339e58d82df43199b7";
        outOfStock(cartId, "update");
        verify(checkoutService, times(0)).partialLock(any(Cart.class), any(ProductInfo.class), anyInt());
    }

    private void outOfStock(String cartId, String cartAction)
    {
        String sku = "mans-46";
        ProductDto dto = getProductDto(sku, cartAction, 10);

        callApiAndVerifyOutOfStock(cartId, dto);
    }

    private void callApiAndVerifyOutOfStock(
            String cartId,
            ProductDto dto)
    {
        webTestClient.patch()
                     .uri(BASE_URL, cartId)
                     .body(Mono.just(dto), ProductDto.class)
                     .accept(Global.JSON)
                     .exchange()
                     .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                     .expectBody(OutOfStockException.class)
                     .consumeWith(response -> {
                         OutOfStockException out = response.getResponseBody();

                         assertThat(out.getMessage(), equalToIgnoringCase("product is Out Of Stock"));
                     });
    }

    private void callApiAndVerifyPartialOutOfStock(
            String cartId,
            BigDecimal productAmount,
            BigDecimal cartExpectedAmount,
            ProductDto dto,
            Map<String, Integer> productExpectation,
            Map<String, Integer> resExpectation)
    {
        webTestClient.patch()
                     .uri(BASE_URL, cartId)
                     .body(Mono.just(dto), ProductDto.class)
                     .accept(Global.JSON)
                     .exchange()
                     .expectStatus().isEqualTo(HttpStatus.PARTIAL_CONTENT)
                     .expectBody(PartialOutOfStockException.class)
                     .consumeWith(response -> {
                         PartialOutOfStockException ex = response.getResponseBody();
                         Cart cart = ex.getCart();

                         assertThat(ex.getMessage(), equalToIgnoringCase("This product has only 5 of these available"));
                         assertTrue("Cart is not active ", cart.isActive());
                         assertTrue("cart total not the expected",
                                    cart.getMoney().getAmount().equals(cartExpectedAmount));

                         verifyCartProductInfo(cart, resExpectation, productAmount, true);

                         lockingValidator.productQuantityAndHasReservation(productExpectation,
                                                                           resExpectation,
                                                                           cartId,
                                                                           true);
                     });
    }

    private void callApiAndVerifyPassing(
            String cartId,
            BigDecimal productAmount,
            BigDecimal cartExpectedAmount,
            ProductDto dto,
            Map<String, Integer> productExpectation,
            Map<String, Integer> resExpectation,
            boolean productInfo,
            boolean res)
    {
        webTestClient.patch()
                     .uri(BASE_URL, cartId)
                     .body(Mono.just(dto), ProductDto.class)
                     .accept(Global.JSON)
                     .exchange()
                     .expectStatus().is2xxSuccessful()
                     .expectBody(Cart.class)
                     .consumeWith(response -> {
                         Cart cart = response.getResponseBody();

                         assertTrue("Cart is not active ", cart.isActive());
                         assertTrue("cart total not the expected",
                                    cart.getMoney().getAmount().equals(cartExpectedAmount));

                         verifyCartProductInfo(cart, resExpectation, productAmount, productInfo);

                         lockingValidator.productQuantityAndHasReservation(productExpectation,
                                                                           resExpectation,
                                                                           cartId,
                                                                           res);
                     });
    }

    private void verifyCartProductInfo(
            Cart cart,
            Map<String, Integer> expectation,
            BigDecimal productAmount,
            boolean addOrUpdate)
    {
        expectation.forEach((k, v) -> {
            System.out.println(v);
            Optional<ProductInfo> opt = cart.getProductInfos()
                                            .stream()
                                            .filter(info ->
                                                            info.getSku().equalsIgnoreCase(k)
                                                                    &&
                                                                    info.getVariationId().equalsIgnoreCase(k)
                                                                    && info.getQuantity() == v
                                                                    && info.getMoney().getAmount().equals(productAmount)
                                            ).findFirst();
            if (addOrUpdate)
            {
                assertTrue(String.format(errorMessageTemplate, "no", k, k, v, productAmount),
                           opt.isPresent());
            }
            else
            {
                assertFalse(String.format(errorMessageTemplate, "", k, k, v, productAmount),
                            opt.isPresent());
            }

        });
    }

    private ProductDto getProductDto(String sku, String cartAction, int lock)
    {
        return ProductDto.builder()
                         .sku(sku)
                         .variationId(sku)
                         .cartAction(cartAction)
                         .quantity(lock)
                         .build();
    }

    @Autowired
    public void lockingValidatorInstance(ProductRepository productRepository)
    {
        lockingValidator = new ProductLockingValidator(productRepository);
    }
}
