package com.mans.ecommerce.b2c.domain.entity.customer;

import java.util.ArrayList;
import java.util.List;

import com.mans.ecommerce.b2c.domain.entity.sharedSubEntity.ProductInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = {})
public class Feed
{

    @Id
    private ObjectId id;

    private List<String> clicked;

    private List<ProductInfo> bought;

    private List<ProductInfo> addedToChart;

    public Feed(ObjectId id)
    {
        this.id = id;
        clicked = new ArrayList<>();
        bought = new ArrayList<>();
        addedToChart = new ArrayList<>();
    }

    public String getId()
    {
        return id.toHexString();
    }

}
