package com.mans.ecommerce.b2c.domain.entity.financial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mans.ecommerce.b2c.domain.entity.customer.Cart;
import com.mans.ecommerce.b2c.domain.entity.customer.subEntity.Address;
import com.mans.ecommerce.b2c.domain.entity.financial.subEntity.Financial;
import com.mans.ecommerce.b2c.domain.entity.financial.subEntity.OrderDetail;
import com.mans.ecommerce.b2c.domain.entity.sharedSubEntity.Money;
import com.mans.ecommerce.b2c.domain.enums.Currency;
import com.stripe.model.Charge;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString(exclude = {})
public class Order
{
    @Id
    private ObjectId id;

    private ObjectId CustomerId;

    @CreatedDate
    private LocalDateTime createdOn;

    @LastModifiedDate
    private LocalDateTime updatedOn;

    private Money totalAmount;

    private Financial financial;

    private OrderDetail detail;

    public Order(Cart cart, Address address, Charge charge)
    {

        this.CustomerId = cart.getId();
        this.totalAmount = cart.getMoney();
        this.detail = new OrderDetail(cart, address, charge);
        this.financial = new Financial(new Money(new BigDecimal("0"), Currency.USD));
    }
}
