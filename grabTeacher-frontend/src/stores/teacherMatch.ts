import { defineStore } from 'pinia'

interface MatchFormState {
  subject: string
  grade: string
  preferredWeekdays: number[]
  selectedPeriods: string[]
  gender: string
  teacherLevel: string
  trialSearchDate: string
  preferredStartDate: string
  preferredEndDate: string
}

export const useTeacherMatchStore = defineStore('teacherMatch', {
  state: () => ({
    form: {
      subject: '',
      grade: '',
      preferredWeekdays: [] as number[],
      selectedPeriods: [] as string[],
      gender: '',
      teacherLevel: '',
      trialSearchDate: '',
      preferredStartDate: '',
      preferredEndDate: ''
    } as MatchFormState,
    matchedTeachers: [] as any[],
    showResults: false,
    scrollTop: 0
  }),
  actions: {
    save(payload: { form?: Partial<MatchFormState>; matchedTeachers?: any[]; showResults?: boolean; scrollTop?: number }) {
      if (payload.form) {
        this.form = {
          subject: payload.form.subject ?? this.form.subject,
          grade: payload.form.grade ?? this.form.grade,
          preferredWeekdays: payload.form.preferredWeekdays ? [...payload.form.preferredWeekdays] : [...this.form.preferredWeekdays],
          selectedPeriods: payload.form.selectedPeriods ? [...payload.form.selectedPeriods] : [...this.form.selectedPeriods],
          gender: payload.form.gender ?? this.form.gender,
          teacherLevel: payload.form.teacherLevel ?? this.form.teacherLevel,
          trialSearchDate: payload.form.trialSearchDate ?? this.form.trialSearchDate,
          preferredStartDate: payload.form.preferredStartDate ?? this.form.preferredStartDate,
          preferredEndDate: payload.form.preferredEndDate ?? this.form.preferredEndDate
        }
      }
      if (payload.matchedTeachers) this.matchedTeachers = [...payload.matchedTeachers]
      if (typeof payload.showResults === 'boolean') this.showResults = payload.showResults
      if (typeof payload.scrollTop === 'number') this.scrollTop = payload.scrollTop
    },
    clear() {
      this.form = {
        subject: '',
        grade: '',
        preferredWeekdays: [],
        selectedPeriods: [],
        gender: '',
        teacherLevel: '',
        trialSearchDate: '',
        preferredStartDate: '',
        preferredEndDate: ''
      }
      this.matchedTeachers = []
      this.showResults = false
      this.scrollTop = 0
    }
  }
})

