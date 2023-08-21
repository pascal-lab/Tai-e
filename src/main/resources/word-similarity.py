from nltk.corpus import wordnet as wn
from itertools import product
import re
import sys

while input := sys.stdin.readline():
    tokens = input.split()
    wordsx = re.sub(r"([A-Z])", r" \1", tokens[0]).split()
    wordsy = re.sub(r"([A-Z])", r" \1", tokens[1]).split()

    maxscore = 0
    for wordx in wordsx:
        for wordy in wordsy:
            sem1, sem2 = wn.synsets(wordx), wn.synsets(wordy)
            for i, j in list(product(*[sem1, sem2])):
                score = i.wup_similarity(j)
                maxscore = score if maxscore < score else maxscore
    print(maxscore)
