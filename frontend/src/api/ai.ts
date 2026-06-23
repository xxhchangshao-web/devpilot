import request from './request'

export interface AiSuggestionRequest {
  description: string
  category?: string
}

export interface AiSuggestionResponse {
  investigation: string
  possibleCauses: string[]
}

export const aiApi = {
  /** AI 智能排查建议 */
  suggest(data: AiSuggestionRequest) {
    return request.post<AiSuggestionResponse>('/api/ai/suggest', data)
  }
}
