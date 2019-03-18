import numpy as np
import difflib
from sklearn.feature_extraction.text import CountVectorizer
from scipy.spatial.distance import cosine, jaccard, hamming, euclidean, dice

def analyzer(s):
    tokens = list()
    token = ''
    for c in s:
        if not c.isalnum():
            if token:
                tokens.append(token)
                token = ''
            if c != ' ':
                tokens.append(c)
            continue
        token += c
    return tokens

def strcmp(a: str, b: str, ignore_blank=True):
    if ignore_blank:
        return ''.join(a.split()) == ''.join(b.split())
    else:
        return a == b

def extract_features(faulty_code: str, ingredient_pool: list):
    assert type(faulty_code) is str
    assert type(ingredient_pool) is list
    assert all([elem and type(elem) is str for elem in ingredient_pool])
    #assert all(not strcmp(elem, faulty_code) for elem in ingredient_pool ])
   
    ingredient_pool = [ candidate.strip() for candidate in ingredient_pool ]
    vectorizers = [
        CountVectorizer(ngram_range=(1,1), lowercase=False, binary=False, analyzer=analyzer),
        CountVectorizer(ngram_range=(1,1), lowercase=False, binary=True, analyzer=analyzer),
    ]

    vectors = [ vectorizer.fit_transform([faulty_code] + ingredient_pool).toarray() for vectorizer in vectorizers ]
    
    features = dict()
    max_lcs, max_nmb, max_mbs = 0, 0, 0
   
    for i, candidate in enumerate(ingredient_pool):
        bowsims = [ cosine(vectors[v][i+1], vectors[v][0]) for v in range(len(vectorizers)) ]
        for j, bowsim in enumerate(bowsims):
            if np.isnan(bowsim):
                bowsims[j] = 1.0
        
        seq_matcher = difflib.SequenceMatcher(lambda t: t in ' \t', candidate, faulty_code)
        nmb = len(seq_matcher.get_matching_blocks())
        hd = len(list(filter(lambda op: op[0] != 'equal', seq_matcher.get_opcodes())))
        lcs = seq_matcher.find_longest_match(0, len(candidate), 0, len(faulty_code)).size
        ls = 1 - seq_matcher.ratio()
        mbs = sum(map(lambda b: b.size, seq_matcher.get_matching_blocks()))
        
        # update the maximum values
        max_lcs = max(lcs, max_lcs)
        max_nmb = max(nmb, max_nmb)
        max_mbs = max(mbs, max_mbs)
               
        features[candidate] = bowsims + [ls, hd, lcs, nmb, mbs]

    for candidate in features:
        f = features[candidate]
        f[-3] /= float(max_lcs)
        f[-2] /= float(max_nmb)
        f[-1] /= float(max_mbs)

    return features

if __name__ == "__main__":
    source = '''
import time
from enum import Enum

class TriangleType(Enum):
    INVALID, EQUALATERAL, ISOCELES, SCALENE = 0, 1, 2, 3

def classify_triangle(a, b, c):
    # Sort the sides so that a <= b <= c
    if a > b:
        tmp = a
        a = b
        b = tmp

    if a > c:
        tmp = b
        a = c
        c = tmp

    if b > c:
        tmp = b
        b = c
        c = tmp

    if a + b <= c:
        return TriangleType.INVALID
    elif a == b and b == c:
        return TriangleType.EQUALATERAL
    elif a == b or b == c:
        return TriangleType.ISOCELES
    else:
        return TriangleType.SCALENE

if __name__ == "__main__": # pragma: no cover
    import sys
    classify_triangle(int(sys.argv[1]), int(sys.argv[2]), int(sys.argv[3]))
    '''
    faulty_code = "if a > c:"
    lines = source.strip().split('\n')
    ingredient_pool = list(filter(lambda line: line and not strcmp(faulty_code, line), lines))
    ingredient_pool = list(set(ingredient_pool))

    maxlen = max([ len(line) for line in lines ])
    features = extract_features(faulty_code, ingredient_pool)
    for line in lines:
        print(line + ' '*(maxlen - len(line)), features[line.strip()] if line.strip() in features else '')
