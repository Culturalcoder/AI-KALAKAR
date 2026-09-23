import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.metrics import mean_absolute_error, mean_absolute_percentage_error, r2_score
import joblib

df = pd.read_csv('/mnt/user-data/uploads/1788682253879_handicraft_pricing.csv')
print(df.shape)
print(df.dtypes)
print(df.isna().sum().sum())

cat_cols = ['category','material','craftType','quality']
num_cols = ['rawMaterialCost','laborCost','packagingCost','otherCost','totalCost','competitorPrice','averageMarketPrice','demandScore']
target = 'sellingPrice'

X = df[cat_cols+num_cols]
y = df[target]

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

preprocess = ColumnTransformer([
    ('cat', OneHotEncoder(handle_unknown='ignore'), cat_cols),
], remainder='passthrough')

models = {
    'LinearRegression': LinearRegression(),
    'RandomForest': RandomForestRegressor(n_estimators=300, max_depth=12, random_state=42, n_jobs=-1),
    'GradientBoosting': GradientBoostingRegressor(n_estimators=300, max_depth=3, learning_rate=0.05, random_state=42)
}

results = {}
for name, model in models.items():
    pipe = Pipeline([('prep', preprocess), ('model', model)])
    pipe.fit(X_train, y_train)
    pred = pipe.predict(X_test)
    mae = mean_absolute_error(y_test, pred)
    mape = mean_absolute_percentage_error(y_test, pred)
    r2 = r2_score(y_test, pred)
    results[name] = (mae, mape, r2, pipe)
    print(f"{name}: MAE={mae:.2f}, MAPE={mape*100:.2f}%, R2={r2:.4f}")

best_name = max(results, key=lambda k: results[k][2])
best_pipe = results[best_name][3]
print("Best model:", best_name)

joblib.dump(best_pipe, '/home/claude/pricing_model.joblib')

# Feature importance for RF/GB
model_obj = best_pipe.named_steps['model']
if hasattr(model_obj, 'feature_importances_'):
    ohe = best_pipe.named_steps['prep'].named_transformers_['cat']
    cat_feature_names = list(ohe.get_feature_names_out(cat_cols))
    all_features = cat_feature_names + num_cols
    importances = model_obj.feature_importances_
    imp_df = pd.DataFrame({'feature': all_features, 'importance': importances}).sort_values('importance', ascending=False)
    print(imp_df.head(20))
    imp_df.to_csv('/home/claude/feature_importance.csv', index=False)
