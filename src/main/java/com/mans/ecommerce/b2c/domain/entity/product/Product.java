package com.mans.ecommerce.b2c.domain.entity.product;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mans.ecommerce.b2c.domain.entity.product.subEntity.*;
import com.mans.ecommerce.b2c.domain.entity.sharedSubEntity.ProductInfo;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString(exclude = {})
public class Product
{
    @Id
    private String id;

    @Indexed(unique = true)
    private String sku;

    @JsonProperty(value = "dSku")
    private String dealtVariationSku;

    private BasicInfo basicInfo;

    private Details details;

    private List<VariationLevel> variations;

    private Map<String, Availability> availability; // key=VariationSku if no variation map{this.sku:Availability}

    private Feedbacks feedback;

    private List<ProductInfo> similarItems;
}
