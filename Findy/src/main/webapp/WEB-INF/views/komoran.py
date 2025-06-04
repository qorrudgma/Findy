from konlpy.tag import Komoran

def komoran(text):
    komoran = Komoran()
    pos_result = komoran.pos(text)
    nouns = [word for word, tag in pos_result if tag in ['NNG', 'NNP'] and len(word) > 1]

    return nouns, pos_result