export interface Session {
  accessToken: string
  expiresAt: number
  email: string
  userId: number
}

export type AuthResponse = Omit<Session, 'expiresAt'> & { expiresIn: number }

export interface DictionaryDetails {
  phonetic: string | null
  sourceUrl: string | null
  licenseUrl: string | null
}

export interface Word {
  id: number
  russianText: string
  englishText: string
  example: string | null
  russianHint: string | null
  sourceLanguage: 'en' | 'ru'
  dictionary: DictionaryDetails | null
}

export interface Collection {
  id: number
  name: string
  description: string | null
  wordCount: number
}

export interface Room {
  id: number
  name: string
  description: string | null
  ownerId: number
  moderatorIds: number[]
}

export interface ChatMessage {
  id: number
  roomId: number
  authorId: number
  text: string
  createdAt: string
}

export interface ChatEvent {
  type: string
  roomId: number
  message: ChatMessage | null
}

export type Api = <T>(path: string, method?: string, body?: unknown) => Promise<T>

export interface StudyQueue {
  total: number
  newCards: number
  dueCards: number
  nextDue: string | null
  card: {
    word: Word
    direction: 'en' | 'ru'
    revision: number
    options: { rating: number; due: string }[]
  } | null
}

export interface AnkiPreview {
  noteTypes: { id: number; name: string; fields: string[]; notes: number; samples: string[][] }[]
  totalNotes: number
  unsupportedNotes: number
}

export interface AnkiImportResult {
  collectionId: number
  created: number
  reused: number
  skipped: number
  warnings: string[]
}

export interface Statistics {
  users: number
  rooms: number
  messages: number
  words: number
  uniqueWords: number
}
