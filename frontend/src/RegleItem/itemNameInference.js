import { resolveItemTypeOptions } from './itemCategoryStore'
import { normalizeItemType, resolveSubcategoryOptions } from './subcategoryStore'

const MIN_TYPE_SCORE = 6
const MIN_SUBCATEGORY_SCORE = 6

const TYPE_KEYWORDS = {
  SNEAKER: [
    'sneaker',
    'sneakers',
    'basket',
    'baskets',
    'chaussure',
    'chaussures',
    'nike',
    'adidas',
    'jordan',
    'air jordan',
    'dunk',
    'yeezy',
    'new balance',
    'nb 2002r',
    '2002r',
    '1906r',
    'asics',
    'salomon',
    'air max',
    'air force',
  ],
  CLOTHING: [
    'vetement',
    'vetements',
    'hoodie',
    'sweat',
    't-shirt',
    'tshirt',
    'tee shirt',
    'veste',
    'jacket',
    'pantalon',
    'jean',
    'short',
    'casquette',
    'maillot',
    'chemise',
  ],
  ACCESSORY: [
    'accessoire',
    'accessoires',
    'sac',
    'bag',
    'bijou',
    'bijoux',
    'lunettes',
    'ceinture',
    'wallet',
    'portefeuille',
    'porte cartes',
  ],
  WATCH: [
    'montre',
    'watch',
    'rolex',
    'omega',
    'seiko',
    'casio',
    'g-shock',
    'g shock',
    'cartier',
    'tudor',
    'submariner',
    'datejust',
  ],
  ELECTRONICS: [
    'electronique',
    'iphone',
    'samsung',
    'smartphone',
    'telephone',
    'ps5',
    'playstation',
    'xbox',
    'switch',
    'console',
    'airpods',
    'casque',
    'macbook',
    'ipad',
    'ordinateur',
    'pc portable',
    'appareil photo',
    'camera',
    'objectif',
  ],
  COLLECTIBLE: [
    'collection',
    'collectible',
    'figurine',
    'funko',
    'pop',
    'vinyle',
    'lego',
    'bearbrick',
    'kaws',
    'art toy',
    'edition limitee',
  ],
  HOME: [
    'maison',
    'mobilier',
    'meuble',
    'chaise',
    'table',
    'canape',
    'lampe',
    'luminaire',
    'decoration',
    'deco',
    'cuisine',
    'etagere',
  ],
  POKEMON_CARD: [
    'pokemon',
    'carte pokemon',
    'cartes pokemon',
    'pikachu',
    'charizard',
    'dracaufeu',
    'booster',
    'display pokemon',
    'etb',
    'psa pokemon',
    'cgc pokemon',
  ],
  TICKET: [
    'ticket',
    'tickets',
    'billet',
    'place concert',
    'concert',
    'festival',
    'match',
    'theatre',
    'spectacle',
    'arena',
    'stade',
  ],
}

const SUBCATEGORY_KEYWORDS = {
  SNEAKER: {
    jordan: ['jordan', 'air jordan', 'aj1', 'aj3', 'aj4', 'aj5', 'aj11'],
    dunk: ['dunk', 'sb dunk', 'nike sb'],
    yeezy: ['yeezy', 'boost 350', 'foam runner', 'slide yeezy'],
    running: ['running', 'runner', 'gel kayano', 'gel lyte', 'vaporfly', 'alphafly'],
    'new balance': ['new balance', 'nb', '2002r', '1906r', '990', '991', '992', '993', '9060'],
    asics: ['asics', 'gel kayano', 'gel lyte', 'gel nyc'],
  },
  CLOTHING: {
    hoodie: ['hoodie', 'sweat', 'sweatshirt', 'pull'],
    't shirt': ['t-shirt', 'tshirt', 'tee shirt', 'maillot'],
    veste: ['veste', 'jacket', 'blouson'],
    pantalon: ['pantalon', 'jean', 'cargo', 'short'],
    casquette: ['casquette', 'cap', 'bonnet'],
  },
  ACCESSORY: {
    sac: ['sac', 'bag', 'backpack', 'banane'],
    bijou: ['bijou', 'bijoux', 'bracelet', 'collier', 'bague'],
    lunettes: ['lunettes', 'sunglasses'],
    ceinture: ['ceinture', 'belt'],
  },
  WATCH: {
    automatique: ['automatique', 'automatic', 'rolex', 'omega', 'seiko', 'tudor'],
    quartz: ['quartz'],
    connectee: ['connectee', 'smartwatch', 'apple watch', 'galaxy watch'],
    vintage: ['vintage'],
  },
  ELECTRONICS: {
    console: ['console', 'ps5', 'playstation', 'xbox', 'switch', 'nintendo'],
    smartphone: ['smartphone', 'telephone', 'iphone', 'samsung', 'pixel'],
    audio: ['audio', 'airpods', 'casque', 'ecouteurs', 'enceinte'],
    photo: ['photo', 'camera', 'appareil photo', 'objectif', 'canon', 'sony alpha', 'nikon'],
    ordinateur: ['ordinateur', 'macbook', 'pc portable', 'laptop', 'ipad', 'tablette'],
  },
  COLLECTIBLE: {
    figurine: ['figurine', 'funko', 'pop'],
    carte: ['carte', 'card'],
    vinyle: ['vinyle', 'vinyl'],
    'art toy': ['art toy', 'bearbrick', 'kaws'],
    'edition limitee': ['edition limitee', 'limited edition'],
  },
  HOME: {
    mobilier: ['mobilier', 'meuble', 'chaise', 'table', 'canape', 'etagere'],
    decoration: ['decoration', 'deco', 'cadre', 'vase'],
    luminaire: ['luminaire', 'lampe'],
    cuisine: ['cuisine', 'vaisselle'],
  },
  POKEMON_CARD: {
    booster: ['booster', 'pack'],
    display: ['display', 'boite display'],
    'carte gradee': ['carte gradee', 'psa', 'cgc', 'bgs', 'grade'],
    coffret: ['coffret', 'box collection'],
    etb: ['etb', 'elite trainer box'],
  },
  TICKET: {
    concert: ['concert', 'arena', 'accor arena', 'zenith'],
    'evenement sportif': ['match', 'sportif', 'stade', 'football', 'basket', 'tennis'],
    festival: ['festival'],
    theatre: ['theatre'],
    spectacle: ['spectacle', 'show'],
  },
}

function normalizeSearchText(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('fr')
    .replace(/[^a-z0-9]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function normalizeAliasKey(value) {
  return normalizeSearchText(value)
}

function buildSearch(value) {
  const text = normalizeSearchText(value)
  const words = new Set(text.split(' ').filter(Boolean))
  return {
    text,
    paddedText: ` ${text} `,
    words,
  }
}

function phraseScore(search, phrase) {
  const normalized = normalizeSearchText(phrase)
  if (!normalized || normalized.length < 2) return 0

  const tokens = normalized.split(' ').filter(Boolean)
  if (search.paddedText.includes(` ${normalized} `)) {
    if (tokens.length > 1) return 10
    return normalized.length <= 2 ? 4 : 7
  }

  if (tokens.length > 1 && tokens.every((token) => search.words.has(token))) {
    return 6
  }

  return 0
}

function scoreAliases(search, aliases = []) {
  let score = 0
  for (const alias of aliases) {
    score += phraseScore(search, alias)
  }
  return score
}

function scoreType(search, option) {
  const type = normalizeItemType(option?.value)
  const keywords = TYPE_KEYWORDS[type] || []
  const labelScore = scoreAliases(search, [option?.label, option?.defaultLabel]) * 0.75
  const keywordScore = scoreAliases(search, keywords)
  return labelScore + keywordScore
}

function subcategoryAliases(type, option) {
  const itemType = normalizeItemType(type)
  const key = normalizeAliasKey(option)
  return [option, ...(SUBCATEGORY_KEYWORDS[itemType]?.[key] || [])]
}

function inferSubcategory(search, type, options = {}) {
  const itemType = normalizeItemType(type)
  const candidates = resolveSubcategoryOptions(itemType, {
    stored: options.storedSubcategories,
    discovered: options.discoveredSubcategories,
    currentValue: options.currentSubcategory,
    mainCategoryAliases: options.mainCategoryAliases,
    categoryLabels: options.categoryLabels,
  })

  let best = { value: '', score: 0 }
  for (const candidate of candidates) {
    const score = scoreAliases(search, subcategoryAliases(itemType, candidate))
    if (score > best.score) best = { value: candidate, score }
  }

  return best.score >= MIN_SUBCATEGORY_SCORE ? best : { value: '', score: best.score }
}

export function inferItemClassificationFromName(name, options = {}) {
  const search = buildSearch(name)
  if (search.text.length < 2) {
    return { type: '', subcategory: '', typeScore: 0, subcategoryScore: 0 }
  }

  const typeOptions = resolveItemTypeOptions(options.categoryLabels)
  let bestType = { value: '', score: 0 }

  for (const option of typeOptions) {
    const score = scoreType(search, option)
    if (score > bestType.score) {
      bestType = { value: normalizeItemType(option.value), score }
    }
  }

  const type = bestType.score >= MIN_TYPE_SCORE ? bestType.value : ''
  const subcategoryType = type || normalizeItemType(options.currentType)
  const bestSubcategory = subcategoryType
    ? inferSubcategory(search, subcategoryType, options)
    : { value: '', score: 0 }

  return {
    type,
    subcategory: bestSubcategory.value,
    typeScore: bestType.score,
    subcategoryScore: bestSubcategory.score,
  }
}
