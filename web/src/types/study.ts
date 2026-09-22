export interface StudySet {
  id: number
  title: string
  description: string
}

export interface CourseSection {
  id: number
  title: string
}

export interface ConceptProgress {
  materialId: number
  concept: string
  attempts: number
  correctAnswers: number
  averageScore: number
  masteryScore: number
  lastReviewedAt: string | null
}

export interface SectionProgress {
  sectionId: number
  concepts: ConceptProgress[]
}

export type QuestionType =
  | 'MULTIPLE_CHOICE'
  | 'TRUE_FALSE'
  | 'SHORT_ANSWER'

export interface StudyQuestion {
  questionId: string
  questionNumber: number
  materialId: number
  type: QuestionType
  question: string
  options: string[]
}

export interface StudySession {
  sessionId: string
  sectionId: number
  questionCount: number
  questions: StudyQuestion[]
}

export interface CreateStudySessionRequest {
  questionCount: number
  questionTypes: QuestionType[]
}

export interface StudyMaterial {
  id: number
  concept: string
  content: string
  createdAt: string
}

export interface StudyMaterialRequest {
  concept: string
  content: string
}

export interface AnswerRequest {
  selectedOptionIndex: number | null
  answer: string | null
}

export interface AnswerResult {
  correct: boolean
  score: number
  feedback: string
  explanation: string
  expectedAnswer: string
  correctConcepts: string[]
  missingConcepts: string[]
}