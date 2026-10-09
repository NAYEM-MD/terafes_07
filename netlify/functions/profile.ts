import { getUser } from '@netlify/identity';
import type { Config } from '@netlify/functions';
import { eq } from 'drizzle-orm';
import { db } from '../../db/index';
import { userProfiles } from '../../db/schema';
import { mergeProfile, validateOnboardingProfile } from '../../src/lib/accountProfile';
import type { UserProfile } from '../../src/types/user';

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type': 'application/json',
      'Cache-Control': 'no-store',
    },
  });
}

function toRecord(
  userId: string,
  email: string,
  row?: {
    onboardingCompleted: boolean;
    onboardingCompletedAt: Date | null;
    profile: UserProfile | null;
  },
) {
  return {
    userId,
    email,
    onboardingCompleted: row?.onboardingCompleted ?? false,
    onboardingCompletedAt: row?.onboardingCompletedAt
      ? row.onboardingCompletedAt.toISOString()
      : null,
    profile: row?.profile ? { ...row.profile, id: userId } : null,
  };
}

export default async function profileHandler(req: Request) {
  const authUser = await getUser();
  if (!authUser?.id) {
    return json({ error: 'Unauthorized' }, 401);
  }

  const userId = authUser.id;
  const email = authUser.email ?? '';

  try {
    if (req.method === 'GET') {
      const [row] = await db
        .select()
        .from(userProfiles)
        .where(eq(userProfiles.userId, userId))
        .limit(1);
      return json(toRecord(userId, row?.email || email, row));
    }

    if (req.method === 'PUT') {
      const body = (await req.json().catch(() => null)) as {
        profile?: Partial<UserProfile>;
        complete?: boolean;
      } | null;
      if (!body?.profile || typeof body.profile !== 'object') {
        return json({ error: 'Profile payload is required.' }, 400);
      }

      const [existing] = await db
        .select()
        .from(userProfiles)
        .where(eq(userProfiles.userId, userId))
        .limit(1);

      const merged = mergeProfile(userId, existing?.profile ?? null, body.profile);
      const complete = Boolean(body.complete);

      if (complete) {
        const errors = validateOnboardingProfile(merged);
        if (errors.length > 0) {
          return json({ error: errors[0], errors }, 400);
        }
      }

      const alreadyComplete = existing?.onboardingCompleted === true;
      const onboardingCompleted = alreadyComplete || complete;
      const onboardingCompletedAt = alreadyComplete
        ? existing?.onboardingCompletedAt ?? null
        : complete
          ? new Date()
          : null;
      const now = new Date();

      const [saved] = await db
        .insert(userProfiles)
        .values({
          id: existing?.id ?? crypto.randomUUID(),
          userId,
          email,
          onboardingCompleted,
          onboardingCompletedAt,
          profile: merged,
          createdAt: existing?.createdAt ?? now,
          updatedAt: now,
        })
        .onConflictDoUpdate({
          target: userProfiles.userId,
          set: {
            email,
            onboardingCompleted,
            onboardingCompletedAt,
            profile: merged,
            updatedAt: now,
          },
        })
        .returning();

      return json(toRecord(userId, email, saved));
    }

    return json({ error: 'Method not allowed' }, 405);
  } catch (error) {
    console.error('Profile persistence failed', error);
    return json(
      {
        error:
          'Profile storage is unavailable. Confirm Netlify Database is provisioned for this site and try again.',
      },
      503,
    );
  }
}

export const config: Config = {
  path: '/api/v1/users/me',
  method: ['GET', 'PUT'],
};
