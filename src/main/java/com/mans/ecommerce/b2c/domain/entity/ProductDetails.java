package com.mans.ecommerce.b2c.domain.entity;

import java.util.List;
import java.util.Map;

import com.mans.ecommerce.b2c.domain.entity.subEntity.Review;

public class ProductDetails
{
    private String productId;
    private String shortDescription;
    //private  manufacturerInfo;

    private ProductVariation productVariations;

    private String priceType;
    private double price;

    private int numOfQuestions;
    private List<QAndA> topQAndA;

    private int numReviews;
    private List<Review> topReviews;

    private String description;

    private class ProductVariation
    {
        private Map<String,List<String>> variation;
        private List<AvailableVariation> availableVariation;

        private class AvailableVariation
        {
            private List<String> availableCombination;
            private double price;
            private int quantity;
            private String description;
        }
    }
}
