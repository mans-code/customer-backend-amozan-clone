package com.mans.ecommerce.b2c.domain.entity.financial.subEntity;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = {})
public class OrderHistory
{
    private String status; //TODO enum

    private String desc;

    private String note;

    private Date cratedOn;

    private Date updateOn;
}