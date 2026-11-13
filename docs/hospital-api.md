# CareOS-HMS — Hospital Administration API Reference (Phase 3)

## Base URL
`/api/v1`

---

## 1. Hospital Management (`/hospitals`)

### Register New Hospital
- **POST** `/hospitals`
- **Security:** Public or Platform Admin (`PLATFORM_HOSPITALS_MANAGE` if authenticated)
- **Request Body:**
```json
{
  "hospitalName": "Metro Care Hospital",
  "hospitalCode": "HOSP-METRO",
  "registrationNumber": "MH-2024-REG-001",
  "hospitalType": "GENERAL",
  "email": "contact@metrocare.local",
  "primaryContactNumber": "+919876543210",
  "alternateContactNumber": "+919876543211",
  "website": "https://metrocare.local",
  "addressLine1": "100 Health Way",
  "city": "Mumbai",
  "district": "Mumbai City",
  "state": "Maharashtra",
  "country": "India",
  "pinCode": "400001",
  "adminUsername": "metro.admin",
  "adminEmail": "admin@metrocare.local",
  "adminPassword": "SecurePassword!123",
  "adminFirstName": "Rajesh",
  "adminLastName": "Sharma"
}
```

### List Hospitals (Platform Oversight)
- **GET** `/hospitals?status=ACTIVE&search=Metro&page=0&size=20`
- **Security:** Platform Admin (`PLATFORM_HOSPITALS_MANAGE`)

### Get Current Hospital Profile
- **GET** `/hospitals/me`
- **Security:** Hospital Admin (`HOSPITAL_PROFILE_UPDATE`)

### Update Current Hospital Profile
- **PUT** `/hospitals/me`
- **Security:** Hospital Admin (`HOSPITAL_PROFILE_UPDATE`)

### Update Hospital Status
- **PATCH** `/hospitals/{publicId}/status`
- **Security:** Platform Admin (`PLATFORM_HOSPITALS_MANAGE`)
- **Request Body:**
```json
{
  "status": "SUSPENDED",
  "reason": "Annual compliance documentation pending renewal."
}
```

---

## 2. Hospital Settings (`/hospitals/me/settings`)

### Get Operational Settings
- **GET** `/hospitals/me/settings`
- **Security:** Hospital Admin (`HOSPITAL_PROFILE_UPDATE`)

### Update Operational Settings
- **PUT** `/hospitals/me/settings`
- **Security:** Hospital Admin (`HOSPITAL_PROFILE_UPDATE`)

---

## 3. Clinical Departments (`/departments`)

### List Departments
- **GET** `/departments`
- **Security:** Hospital User (`HOSPITAL_DEPARTMENTS_MANAGE`)

### Create Department
- **POST** `/departments`
- **Security:** Hospital User (`HOSPITAL_DEPARTMENTS_MANAGE`)

### Update Department
- **PUT** `/departments/{publicId}`
- **Security:** Hospital User (`HOSPITAL_DEPARTMENTS_MANAGE`)

### Toggle Department Status
- **PATCH** `/departments/{publicId}/status`
- **Security:** Hospital User (`HOSPITAL_DEPARTMENTS_MANAGE`)

---

## 4. Hospital Branches (`/branches`)

### List Branches
- **GET** `/branches`
- **Security:** Hospital User (`HOSPITAL_BRANCHES_MANAGE`)

### Create Branch
- **POST** `/branches`
- **Security:** Hospital User (`HOSPITAL_BRANCHES_MANAGE`)

### Update Branch
- **PUT** `/branches/{publicId}`
- **Security:** Hospital User (`HOSPITAL_BRANCHES_MANAGE`)

### Toggle Branch Status
- **PATCH** `/branches/{publicId}/status`
- **Security:** Hospital User (`HOSPITAL_BRANCHES_MANAGE`)

---

## 5. Hospital Contacts (`/hospitals/me/contacts`)

### List Contacts
- **GET** `/hospitals/me/contacts`
- **Security:** Hospital User (`HOSPITAL_CONTACTS_MANAGE`)

### Add Contact
- **POST** `/hospitals/me/contacts`
- **Security:** Hospital User (`HOSPITAL_CONTACTS_MANAGE`)

### Update Contact
- **PUT** `/hospitals/me/contacts/{publicId}`
- **Security:** Hospital User (`HOSPITAL_CONTACTS_MANAGE`)

### Delete Contact
- **DELETE** `/hospitals/me/contacts/{publicId}`
- **Security:** Hospital User (`HOSPITAL_CONTACTS_MANAGE`)

---

## 6. Compliance Documents (`/hospitals/me/documents`)

### Upload Compliance Document
- **POST** `/hospitals/me/documents` (Multipart: `file`, `documentType`, `expiryDate`)
- **Security:** Hospital User (`HOSPITAL_DOCUMENTS_MANAGE`)

### Download Document
- **GET** `/hospitals/me/documents/{publicId}/download`
- **Security:** Hospital User / Platform Super Admin

### Verify Document (Platform Approval)
- **PATCH** `/hospitals/{hospitalPublicId}/documents/{documentPublicId}/verify`
- **Security:** Platform Admin (`PLATFORM_DOCUMENTS_VERIFY`)
- **Request Body:**
```json
{
  "verificationStatus": "VERIFIED"
}
```

---

## 7. Audit Logging (`/audit-logs`)

### Query Audit Logs
- **GET** `/audit-logs?page=0&size=50`
- **Security:** `PLATFORM_SUPER_ADMIN` (all tenants) or `HOSPITAL_ADMIN` (scoped to current hospital).