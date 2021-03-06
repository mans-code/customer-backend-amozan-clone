package com.mans.ecommerce.b2c.domain.entity.product.subEntity;

import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = {})
public class Answer
{
    private String answer;

    private String customerId;

    private String by;

    private Date createdOn;
}
