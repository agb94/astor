import json
import sys
import pickle

def extract_features(target, candidates):
    return null
    
with open(sys.argv[1], 'r') as f:
    data = json.load(f)

modi_point = data['ModificationPoint']
ingredients = data['Ingredients']
print("Total ingredients: {}".format(len(ingredients))) 

X = extract_features(modi_point, ingredients)

clf = pickle.load(open('../ASE19/model_NN50d_1547464892.sav', 'rb'))
p = clf.predict(X)
proba = clf.predict_proba(X)

print("blabla")
