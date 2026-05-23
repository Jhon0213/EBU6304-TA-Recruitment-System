# BUPT International School TA Recruitment System

## Codex Implementation Guide

This document is the reusable implementation brief for AI-assisted work on this repository.

Use it as the default source of truth for planning, coding, testing, and documentation.

## 1. Working Rules

- Follow the handout and system specification without changing the technical route.
- Before any code change, provide a short plan.
- Implement the smallest working increment first, then add enhancements.
- After each phase, run build/tests and report the result.
- Keep the code testable, maintainable, and documented.
- Use the Figma design as the main UI reference.
- Do not redesign screens unless JavaFX technical constraints require a minor adaptation.
- If instructions conflict, stop and surface the conflict instead of guessing.

## 2. Non-Negotiable Constraints

- Language: Java 17
- UI: JavaFX
- App type: stand-alone desktop application
- Storage: JSON, CSV, and TXT only
- No database
- No Spring Boot as the primary solution
- Architecture: `ui / controller / service / repository / model / util`
- Core workflow first, enhancements second
- Every stage must be verifiable with build/test output
- AI-style features must remain explainable and rule-based

## 3. Product Definition

This is a bespoke desktop system for BUPT International School TA recruitment.

Roles:

- TA Applicant
- Module Organiser (MO)
- Admin

Core user goals:

- TA: create profile, maintain structured CV data, browse jobs, apply, and track application status
- MO: post jobs, review applicants, accept/reject applicants
- Admin: monitor total workload and export reports

## 4. Scope Priorities

Implement the core workflow before any enhancement:

1. Login and role-based routing
2. TA profile and structured resume data
3. Job browse/search/detail/apply
4. My applications and status tracking
5. MO job management and applicant review
6. Admin workload monitor and CSV export
7. Match score, missing skills, workload warning, explainable panels

## 5. UI Reference

Use the following Figma pages as the primary source for layout, hierarchy, and component grouping.

Design priority:

1. Match layout structure and information hierarchy
2. Match component grouping and page states
3. Match overall visual language
4. Make only minor JavaFX-driven adaptations when needed

Figma pages:

- Design System Overview: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=4-1128&m=dev
- Login: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=4-1405&m=dev
- TA Dashboard: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=4-1484&m=dev
- TA My CV: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=6-2574&m=dev
- TA Edit Profile: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=5-1807&m=dev
- TA Browse Jobs - Detail: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=8-3730&m=dev
- TA Browse Jobs: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=8-5485&m=dev
- TA My Application - Empty: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=10-6995&m=dev
- TA My Application: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=10-7726&m=dev
- MO Job Management - Empty: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=13-8442&m=dev
- MO Job Management: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=13-9877&m=dev
- MO Edit Job Post: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=13-8936&m=dev
- MO Job Applicants - Empty: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=18-11550&m=dev
- MO Job Applicants: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=18-12102&m=dev
- MO Job Applicants - Detail: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=26-2&m=dev
- ADMIN Dashboard: https://www.figma.com/design/0cs8Kmn1KPVLB1oCiJdRnj/Untitled?node-id=27-404&m=dev

If direct Figma access is unavailable, follow the page names, layout hierarchy, and design system as closely as possible.

## 6. Page Map

Main pages:

- P01 Login
- P02 Main Shell
- P03 TA Dashboard
- P04 Applicant Profile
- P05 Resume Information Form
- P06 Job Browser
- P07 Job Detail and Apply
- P08 My Applications
- P09 Job Management
- P10 Job Editor
- P11 Applicant List
- P12 Applicant Review
- P13 Admin Dashboard

Shared dialogs:

- D01 Validation Error
- D02 Success Feedback
- D03 Confirm Action
- D04 Permission Denied
- D05 Explainable Match or Workload Warning

If a page has both normal and empty states in Figma, implement both states.

## 7. Architecture

Use a layered design with clear responsibilities.

### 7.1 Layers

- UI layer: JavaFX views, reusable components, dialogs
- Controller layer: page controllers, event handlers, navigation coordination
- Service layer: business rules, validation, orchestration, state transitions
- Repository layer: JSON/CSV/TXT read and write
- Domain layer: models, enums, DTOs
- Utility layer: validators, date helpers, ID generation, file helpers, export helpers

### 7.2 Suggested directory structure

```text
src/
  main/
    java/
      edu/bupt/ta/
        App.java
        config/
        model/
        enums/
        dto/
        repository/
        service/
        controller/
        ui/
        util/
    resources/
      fxml/
      styles/
      icons/
      sample-data/
  test/
    java/
      repository/
      service/
      controller/
      integration/
```

## 8. Data Storage

Use simple text files only. Do not add a database.

Required runtime files:

- `data/users.json`
- `data/applicant_profiles.json`
- `data/resume_infos.json`
- `data/jobs.json`
- `data/applications.json`
- `data/workloads.json`
- `data/audit_log.txt`
- `data/export/workload_report.csv`
- `data/export/application_report.csv`

Suggested JSON entities:

- `users.json`: userId, username, passwordHash, role, displayName, active
- `applicant_profiles.json`: applicantId, userId, fullName, studentId, programme, year, email, phone, lastUpdated
- `resume_infos.json`: applicantId, relevantModules, technicalSkills, languageSkills, experienceText, personalStatement, availability, maxWeeklyHours, lastUpdated
- `jobs.json`: jobId, title, moduleCode, moduleName, type, description, requiredSkills, preferredSkills, weeklyHours, positions, deadline, organiserId, status, createdAt
- `applications.json`: applicationId, jobId, applicantId, applyDate, statement, status, decisionNote, matchScore, missingSkills
- `workloads.json`: applicantId, acceptedJobIds, currentWeeklyHours, riskLevel
- `audit_log.txt`: timestamp, userId, action, detail

## 9. Domain Model

### 9.1 Enums

- `Role { TA, MO, ADMIN }`
- `JobStatus { DRAFT, OPEN, CLOSED, EXPIRED }`
- `ApplicationStatus { SUBMITTED, UNDER_REVIEW, ACCEPTED, REJECTED }`
- `RiskLevel { LOW, MEDIUM, HIGH }`
- `JobType { MODULE_TA, INVIGILATION, ACTIVITY_SUPPORT, OTHER }`

### 9.2 Model classes

- `User`
- `ApplicantProfile`
- `ResumeInfo`
- `Job`
- `Application`
- `Workload`
- `AuditLogEntry`

### 9.3 DTOs

- `JobListItemDTO`
- `ApplicantListItemDTO`
- `WorkloadSummaryDTO`
- `MatchExplanationDTO`
- `DashboardSummaryDTO`
- `ApplicantReviewDTO`
- `JobSearchCriteria`
- `LoginResult`

## 10. Repository Rules

Repositories must not leak file access into controllers.

Base interface:

```java
public interface Repository<T, ID> {
    List<T> findAll();
    Optional<T> findById(ID id);
    void save(T entity);
    void saveAll(List<T> entities);
    void deleteById(ID id);
}
```

Concrete repositories:

- `UserRepository`
- `ApplicantProfileRepository`
- `ResumeInfoRepository`
- `JobRepository`
- `ApplicationRepository`
- `WorkloadRepository`
- `AuditLogRepository`

Important query methods:

- `findByUsername`
- `findByOrganiserId`
- `findOpenJobs`
- `search`
- `findByApplicantId`
- `findByJobId`
- `findByJobIdAndApplicantId`
- `findByApplicantId` for workloads

## 11. Service Responsibilities

### AuthenticationService

- Login validation
- Current user session
- Role-based routing support

### ApplicantProfileService

- Create or update applicant profile
- Calculate profile completeness

### ResumeService

- Save structured resume data
- Validate resume fields
- Calculate resume completeness

### JobService

- Create, edit, close, and query jobs
- Refresh expired job statuses

### ApplicationService

- Apply for jobs
- Block duplicate or invalid applications
- Query application records

### ReviewService

- Accept or reject applications
- Save decision notes
- Trigger workload updates

### WorkloadService

- Calculate current and projected weekly hours
- Calculate risk level
- Refresh workload summary

### MatchingService

- Calculate match score
- Return explainable match data
- Identify missing skills

### ExportService

- Export workload report CSV
- Export application report CSV

## 12. Roles and Permissions

### TA Applicant

Allowed:

- Dashboard
- Applicant Profile
- Resume Information
- Job Browser
- Job Detail and Apply
- My Applications

Not allowed:

- Job Management
- Applicant Review
- Admin Dashboard

### MO

Allowed:

- Job Management
- Job Editor
- Applicant List
- Applicant Review

Not allowed:

- Admin Dashboard
- Editing jobs owned by another MO
- Reviewing applicants outside owned jobs

### Admin

Allowed:

- Admin Dashboard
- Read-only system stats and exports

Not allowed:

- Direct accept/reject decisions
- Editing applicant decisions

## 13. Business Rules

- One applicant cannot apply to the same job twice.
- `CLOSED` and `EXPIRED` jobs cannot be applied to.
- Only `OPEN` jobs should show the Apply action.
- MO can only manage jobs they created.
- Admin only monitors and exports.
- Accepting an application must update workload.
- Rejecting an application must not update workload.
- Every save action should append an audit log entry.
- Resume information is the primary CV data source.
- PDF upload is not a core requirement.
- Job deletion is optional and should be avoided if it adds complexity.

## 14. State Transitions

### Job status

- `DRAFT -> OPEN -> CLOSED`
- `OPEN -> EXPIRED`

Rules:

- Expired jobs must be blocked automatically after deadline.
- Closed jobs cannot be applied to.
- Expired jobs cannot be applied to.

### Application status

- `SUBMITTED -> UNDER_REVIEW -> ACCEPTED`
- `SUBMITTED -> UNDER_REVIEW -> REJECTED`
- A simplified version may accept or reject directly from `SUBMITTED`.

### Workload risk

- `LOW`: projected hours <= 80% of max
- `MEDIUM`: 80% < projected hours <= 100% of max
- `HIGH`: projected hours > 100% of max

### Match score

Use a rule-based and explainable scoring model.

Suggested weighting:

- Required skills: 60%
- Preferred skills: 20%
- Relevant modules or experience: 10%
- Availability fit: 10%

The explanation must show:

- matched skills
- missing skills
- current workload
- projected workload

## 15. Validation Rules

### Login

- Username required
- Password required
- Username must exist
- Password must match

### Applicant profile

- Full name required
- Student ID required
- Email must be valid
- Phone is optional but must match format if present

### Resume

- `maxWeeklyHours > 0`
- Availability must not be empty
- `personalStatement` length <= 500 characters

### Job

- Title required
- Module code required
- Module name required
- `weeklyHours > 0`
- `positions > 0`
- Deadline required and valid

### Application

- Applicant must have profile and resume before applying
- Duplicate application must be blocked
- Job status must be `OPEN`

## 16. Sample Data Minimums

Generate demo-ready sample data:

- At least 5 TA users
- At least 2 MO users
- 1 Admin user
- At least 8 jobs
- Jobs must cover `OPEN`, `CLOSED`, `EXPIRED`, and `DRAFT`
- At least 10 applications
- Applications must cover `SUBMITTED`, `UNDER_REVIEW`, `ACCEPTED`, and `REJECTED`
- At least 5 workload records
- Workload risk levels must cover `LOW`, `MEDIUM`, and `HIGH`

## 17. Testing Requirements

### Unit tests

Cover at least:

- `AuthenticationServiceTest`
- `ApplicantProfileServiceTest`
- `ResumeServiceTest`
- `JobServiceTest`
- `ApplicationServiceTest`
- `ReviewServiceTest`
- `WorkloadServiceTest`
- `MatchingServiceTest`
- `JsonRepositoryTest`

### Integration tests

Cover at least:

1. create job -> save -> reload
2. apply job -> application visible
3. accept application -> workload updated
4. close job -> no further application
5. expired job -> blocked
6. export CSV -> file generated

### Manual acceptance script

1. TA logs in and completes profile and resume
2. TA searches jobs and applies
3. MO publishes a job
4. MO reviews applicants
5. Admin checks workload
6. Invalid input shows a validation error
7. Duplicate apply is blocked
8. Overload warning is triggered

## 18. Delivery Artifacts

The repository should be able to generate:

- Source code
- Test programs
- JavaDocs
- README
- User manual
- Sample data
- CSV exports

Recommended output locations:

- `target/site/apidocs/`
- `data/export/`
- `docs/User-Manual.md`
- `docs/screenshots/`

## 19. Development Phases

Use phased delivery and keep each phase runnable.

### Phase 1

- Project skeleton
- Repositories
- Sample data

### Phase 2

- Login
- Job browser
- Job detail
- Apply flow

### Phase 3

- Profile
- Resume
- My applications

### Phase 4

- Review
- Workload
- Admin dashboard

### Phase 5

- Match features
- Workload warning
- Tests
- JavaDocs
- README
- Manual

## 20. AI Execution Contract

When asked to implement work in this repo, follow this order:

1. Restate the scope in a short plan.
2. Implement the smallest useful change.
3. Run the relevant build or test command.
4. Report what changed and whether verification passed.
5. Call out any risk, missing data, or blocked item.

Do not skip verification unless the user explicitly asks for a doc-only change or other non-code task.

## 21. Current Task Brief Template

Use this section for the next task if you want a focused prompt.

```text
Goal:
In scope:
Out of scope:
Files to change:
Expected result:
Verification:
```

