CREATE TABLE "user_profiles" (
	"id" text PRIMARY KEY,
	"user_id" text NOT NULL UNIQUE,
	"email" text NOT NULL,
	"onboarding_completed" boolean DEFAULT false NOT NULL,
	"onboarding_completed_at" timestamp with time zone,
	"profile" jsonb,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
