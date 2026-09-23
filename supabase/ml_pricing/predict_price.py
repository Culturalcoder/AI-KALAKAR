"""
Dynamic Pricing Assistant — Prediction Script
-----------------------------------------------
Loads the trained pricing model (pricing_model.joblib) and predicts an
optimal, competitive selling price for a new handicraft product based on
its category, material, craftsmanship type, quality tier, cost breakdown,
competitor pricing, and demand signal.

Usage:
    python predict_price.py

Or import the function in your own app:
    from predict_price import predict_price
    price = predict_price({
        "category": "Pottery & Terracotta",
        "material": "Clay",
        "craftType": "Wheel Pottery",
        "quality": "Standard",
        "rawMaterialCost": 480,
        "laborCost": 280,
        "packagingCost": 70,
        "otherCost": 80,
        "competitorPrice": 1510,
        "averageMarketPrice": 1510,
        "demandScore": 9,
    })
"""

import joblib
import pandas as pd

MODEL_PATH = "pricing_model.joblib"


def predict_price(product: dict) -> float:
    """
    product must contain:
      category, material, craftType, quality (strings)
      rawMaterialCost, laborCost, packagingCost, otherCost (numbers)
      competitorPrice, averageMarketPrice, demandScore (numbers)

    totalCost is derived automatically as the sum of the four cost fields.
    Returns the model's suggested selling price (float, rounded to nearest rupee).
    """
    pipe = joblib.load(MODEL_PATH)

    total_cost = (
        product["rawMaterialCost"]
        + product["laborCost"]
        + product["packagingCost"]
        + product["otherCost"]
    )

    row = pd.DataFrame([{
        "category": product["category"],
        "material": product["material"],
        "craftType": product["craftType"],
        "quality": product["quality"],
        "rawMaterialCost": product["rawMaterialCost"],
        "laborCost": product["laborCost"],
        "packagingCost": product["packagingCost"],
        "otherCost": product["otherCost"],
        "totalCost": total_cost,
        "competitorPrice": product["competitorPrice"],
        "averageMarketPrice": product["averageMarketPrice"],
        "demandScore": product["demandScore"],
    }])

    price = pipe.predict(row)[0]
    return round(float(price), 2)


if __name__ == "__main__":
    example = {
        "category": "Pottery & Terracotta",
        "material": "Clay",
        "craftType": "Wheel Pottery",
        "quality": "Standard",
        "rawMaterialCost": 480,
        "laborCost": 280,
        "packagingCost": 70,
        "otherCost": 80,
        "competitorPrice": 1510,
        "averageMarketPrice": 1510,
        "demandScore": 9,
    }
    print("Suggested selling price: Rs.", predict_price(example))
