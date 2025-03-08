# pun

## Goals

### Identifiability

> Does `pun` limit the vocabulary it uses?

Yep. This keeps the tool from creating puns with weird obscure words terms that would fly over most people's heads.

> Does the vocabulary rely on a dictionary?

Yes. `pun` draws its vocabulary from English Wiktionary entries that are categorized as English lemmas.

> Can `pun` produce puns that don't use lemma forms?

Yes. Even though the source data is just English lemmas, the tool can inflect these words to create puns.

> Does the vocabulary rely on Wikipedia?

Nope. Wikipedia terms minus English Wiktionary English lemmas would mostly just end up with specialized named entities.

> Does `pun` filter vocabulary by word frequency?

Nah. Word frequency alone doesn't determine how identifiable a term is. For example, "ungoogleable" barely registers on frequency lists, but everyone knows what it means because it's derived from "Google."

Plus, frequency filtering gets messy with phrases, which can show up in all sorts of variations and are a pain to count.

Instead, `pun` uses large language models to score how recognizable each word is, then filters based on those scores.
