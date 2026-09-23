"""
Dynamic Pricing Assistant — REST API
--------------------------------------
Wraps the trained pricing model in a FastAPI app so your mobile app
(iOS/Android) can get a price suggestion with a single HTTP request.

SETUP (one time, on your server):
    pip install fastapi uvicorn joblib pandas scikit-learn

RUN (starts a local server on port 8000):
    uvicorn pricing_api:app --host 0.0.0.0 --port 8000

Make sure pricing_model.joblib is in the same folder as this file.

CALL IT FROM YOUR MOBILE APP:
    POST http://YOUR_SERVER_ADDRESS:8000/predict-price
    Content-Type: application/json

    {
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
        "demandScore": 9
    }

    Response:
    {
        "suggestedPrice": 1237.1,
        "totalCost": 910
    }

DEPLOYING SO YOUR APP CAN REACH IT FROM ANYWHERE:
    Running this on your laptop only works while your laptop is on and
    your phone is on the same network. For a real app, deploy this file
    to a small always-on server, for example:
      - Render.com or Railway.app (free tier, easiest, a few clicks)
      - AWS EC2 / Lightsail, Google Cloud Run, or DigitalOcean
    Any of these will give you a public URL like
    https://your-app-name.onrender.com/predict-price
    which you call from Swift (URLSession) or Kotlin/Java (Retrofit or
    OkHttp) exactly like any other REST API.
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import joblib
import pandas as pd

app = FastAPI(title="Dynamic Pricing Assistant")

# Allow your mobile app (or a testing tool) to call this from anywhere.
# Tighten allow_origins to your actual domain once you go live.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

pipe = joblib.load("pricing_model.joblib")


class ProductInput(BaseModel):
    category: str = Field(..., example="Pottery & Terracotta")
    material: str = Field(..., example="Clay")
    craftType: str = Field(..., example="Wheel Pottery")
    quality: str = Field(..., example="Standard")
    rawMaterialCost: float = Field(..., ge=0, example=480)
    laborCost: float = Field(..., ge=0, example=280)
    packagingCost: float = Field(..., ge=0, example=70)
    otherCost: float = Field(..., ge=0, example=80)
    competitorPrice: float = Field(..., ge=0, example=1510)
    averageMarketPrice: float = Field(..., ge=0, example=1510)
    demandScore: int = Field(..., ge=1, le=10, example=9)


class PriceOutput(BaseModel):
    suggestedPrice: float
    totalCost: float


@app.get("/")
def health_check():
    return {"status": "ok", "message": "Dynamic Pricing Assistant is running"}


@app.post("/predict-price", response_model=PriceOutput)
def predict_price(product: ProductInput):
    try:
        total_cost = (
            product.rawMaterialCost
            + product.laborCost
            + product.packagingCost
            + product.otherCost
        )

        row = pd.DataFrame([{
            "category": product.category,
            "material": product.material,
            "craftType": product.craftType,
            "quality": product.quality,
            "rawMaterialCost": product.rawMaterialCost,
            "laborCost": product.laborCost,
            "packagingCost": product.packagingCost,
            "otherCost": product.otherCost,
            "totalCost": total_cost,
            "competitorPrice": product.competitorPrice,
            "averageMarketPrice": product.averageMarketPrice,
            "demandScore": product.demandScore,
        }])

        price = float(pipe.predict(row)[0])
        return PriceOutput(suggestedPrice=round(price, 2), totalCost=round(total_cost, 2))

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
