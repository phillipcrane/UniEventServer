# UniEventServer

This document is the **non-technical** part of DTU Event documentation, for general users. For developer and contributor documentation, see [CONTRIBUTING.md](./CONTRIBUTING.md).

This website will be a central registry for Technical University of Denmark (DTU)'s campus events from bars and cafes. It's called UniEvent rather than DTUEvent because DTU is trademarked. JS/Node/REACT frontend; Java/SpringBoot/MySQL backend. The site pulls through facebook's Graph API from a number of DTU Campus bars and nearby dorm bars. Note that we do not discriminate between Lyngby Campus and Ballerup Campus.

## Authentication Notes

The backend now issues JWT-based access tokens and refresh tokens from `/api/auth/register` and `/api/auth/login`.

Protected routes use bearer tokens in the `Authorization` header:

- Protected: `POST`, `PUT`, `DELETE`, `/admin/**`, and media uploads
- Public: `GET /api/**`, `/api/auth/**`, Swagger/OpenAPI docs, and the existing CORS preflight routes

Environment variables used by the auth system:

- `UNIEVENT_SECURITY_JWT_SECRET`
- `UNIEVENT_SECURITY_JWT_EXPIRATION_MS`
- `UNIEVENT_SECURITY_JWT_REFRESH_SECRET`
- `UNIEVENT_SECURITY_JWT_REFRESH_EXPIRATION_MS`

Refresh tokens are rotated on use and revoked on logout. If a reused refresh token is detected, the whole token family is revoked.

DTU student events are currently fragmented across many Facebook pages (student orgs, bars, dorms, ad‑hoc groups). New and international students especially struggle to discover what is happening without already following 10-20 pages or relying on friends’ “Interested” notifs. UniEvent then provides a single neutral, lightweight, mobile‑friendly web feed aggregating events (initially via mock data + pages where we have admin tokens). A web app (instead of native) keeps scope realistic and instantly accessible.

## Stakeholders

- Students (primary) - need a simple, reliable overview of upcoming social and academic events.
- Organizers (secondary: PF, bars, dorm committees, study orgs) - want increased, predictable reach and less manual promotion overhead.
- DTU administration (tertiary) - benefits from stronger social cohesion & inclusion.

## Team

Or "TonkaProductions". Note that all contribute code.

- Christian, Phillip - Technical Leads, Backend
- Espen (joined later) - Backend
- Akkash, Hannah, Lilian - Frontend
- Ollie (former) - Backend, testing
- Linh (former) - Frontend

## List

Below are the pages for bars at DTU. Note well that some events are not listed through these pages, but those dedicated to social gatherings.

### Bars

- Diagonalen (The Diagonal): <https://www.facebook.com/DiagonalenDTU>
- Diamanten (The Diamond): <https://www.facebook.com/DiamantenDTU>
- Etheren (The Ether): <https://www.facebook.com/EtherenDTU>
- Hegnet (The Fence): <https://www.facebook.com/hegnetdtu>
- S-Huset (S-House): <https://www.facebook.com/shuset.dk>
- Verners Kælder (Verner's Cellar), Ballerup: <https://www.facebook.com/vernerskaelder>

### Dorm Bars Near Lyngby Campus

- Nakkeosten (The Neck Cheese), Ostenfeld Dorm: <https://www.facebook.com/Nakkeosten>
- Saxen (The Sax), Kampsax Dorm: <https://www.facebook.com/kampsax/?locale=da_DK>

### Dorms Further Away From Lyngby Campus

- Række 0 (Row 0), Trørød Dorm, 11 km: <https://www.facebook.com/profile.php?id=100073724250125>
- Falladen (The Fail), P.O: Pedersen Dorm, 5 km: <https://www.facebook.com/POPSARRANGEMENTER/>
- Pauls Ølstue (Paul's Beer Room), Paul Bergsøe Dorm, 5 km: <https://www.facebook.com/p/Pauls-%C3%98lstue-100057429738696/>

### Event Pages

- SenSommerFest (Latesummer Party): <https://www.facebook.com/SenSommerfest>
- Egmont Kollegiets Festival (Egmont Dorm Festival): <https://www.facebook.com/profile.php?id=100063867437478>

### Missing

The dorms below have no dedicated bars, but still have parties over the summer.

- William Demant Dorm, 2 km
- Villum Kann Rasmussen Dorm, 1 km
