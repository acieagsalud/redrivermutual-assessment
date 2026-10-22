export const LOSS_TYPES = [
  { value: 'FIRE', label: 'Fire or smoke' },
  { value: 'WATER_DAMAGE', label: 'Water damage (burst pipe, leak)' },
  { value: 'SEWER_BACKUP', label: 'Sewer backup' },
  { value: 'OVERLAND_FLOOD', label: 'Overland flood' },
  { value: 'WIND_HAIL', label: 'Wind or hail' },
  { value: 'THEFT', label: 'Theft or break-in' },
  { value: 'VANDALISM', label: 'Vandalism' },
  { value: 'OTHER', label: 'Other' },
] as const

export const PROVINCES = [
  { value: 'AB', label: 'Alberta' },
  { value: 'BC', label: 'British Columbia' },
  { value: 'MB', label: 'Manitoba' },
  { value: 'NB', label: 'New Brunswick' },
  { value: 'NL', label: 'Newfoundland and Labrador' },
  { value: 'NS', label: 'Nova Scotia' },
  { value: 'NT', label: 'Northwest Territories' },
  { value: 'NU', label: 'Nunavut' },
  { value: 'ON', label: 'Ontario' },
  { value: 'PE', label: 'Prince Edward Island' },
  { value: 'QC', label: 'Quebec' },
  { value: 'SK', label: 'Saskatchewan' },
  { value: 'YT', label: 'Yukon' },
] as const

/**
 * Flat form state. Address fields use the same dotted names the API returns in its
 * validation errors (e.g. "address.postalCode"), so server errors map straight onto fields.
 */
export type ClaimFormValues = {
  firstName: string
  lastName: string
  email: string
  phone: string
  policyNumber: string
  lossDate: string
  lossType: string
  'address.line1': string
  'address.line2': string
  'address.city': string
  'address.province': string
  'address.postalCode': string
  description: string
}

export type FieldName = keyof ClaimFormValues

export type FieldErrors = Partial<Record<FieldName, string>>

export const EMPTY_FORM: ClaimFormValues = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  policyNumber: '',
  lossDate: '',
  lossType: '',
  'address.line1': '',
  'address.line2': '',
  'address.city': '',
  'address.province': 'MB',
  'address.postalCode': '',
  description: '',
}
