# ecommerce-microservices
A distributed e-commerce platform

src/app/
├── components/                 # Shared UI blocks
│   ├── navbar/                 # Logo + Login Toggle + Cart Signal Badge
│   └── product-card/           # Bootstrap card for the grid
├── pages/                      # Routed views
│   ├── login/                  # Login form (Bootstrap)
│   ├── register/               # Signup form
│   ├── product-list/           # Home: Grid of ProductCards
│   ├── checkout/               # Order summary + "Redirect to Stripe" button
│   ├── order-success/          # Land here after Stripe. Polls Kafka for status.
│   └── order-cancel/           # Land here if user hits "Back" on Stripe page.
├── services/                   # Business Logic
│   ├── auth.service.ts         # JWT storage & Login/Logout
│   ├── cart.service.ts         # Cart state (Angular Signals)
│   ├── product.service.ts      # Fetch catalog from MongoDB/Gateway
│   └── order.service.ts        # Stripe Session creation & status polling
├── guards/
│   └── auth.guard.ts           # Prevents entering /checkout without JWT
├── interceptors/
│   └── auth.interceptor.ts     # Functional: Automatically adds Bearer <JWT>
├── models/
│   ├── product.model.ts
│   ├── order.model.ts
│   └── user.model.ts
├── app.routes.ts               # Path definitions (Home, Login, Success, etc.)
└── app.config.ts               # Global providers (HttpClient, Interceptors)