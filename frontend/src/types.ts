export interface Session {
  accessToken: string
  expiresAt: number
  email: string
  userId: number
}

export interface DictionaryDetails {
  phonetic: string | null
  audioUrl: string | null
  sourceUrl: string | null
  licenseUrl: string | null
  audioSourceUrl: string | null
  audioLicenseUrl: string | null
}

export interface Word {
  id: number
  russianText: string
  englishText: string
  example: string | null
  sourceLanguage: 'en' | 'ru'
  dictionary: DictionaryDetails | null
}

export interface Collection {
  id: number
  name: string
  description: string | null
  words: Word[]
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
