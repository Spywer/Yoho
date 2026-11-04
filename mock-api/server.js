'use strict';

/**
 * Full mock for docs/openapi.yaml.
 * In-memory state resets on restart.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const { URL } = require('url');
const crypto = require('crypto');

const PORT = Number(process.env.PORT || 8787);
const HOST = process.env.HOST || '0.0.0.0';

const catalog = JSON.parse(
  fs.readFileSync(path.join(__dirname, 'data', 'catalog.json'), 'utf8')
).items;

/** @type {{ token: string|null, user: object|null }} */
const auth = { token: null, user: null };

const store = {
  diary: /** @type {any[]} */ ([]),
  favorites: /** @type {any[]} */ ([]),
  wishlist: /** @type {any[]} */ ([]),
  history: /** @type {any[]} */ ([]),
  comments: /** @type {Record<string, any[]>} */ ({}),
  ratings: /** @type {Record<string, any>} */ ({}),
  ideas: [
    {
      id: 1,
      title: 'Тёмная тема по умолчанию',
      description: 'Уже есть, но оставим как пример',
      votes: 10,
      status: 'implemented',
      author: { id: 'mock-user-1', username: 'spywer', avatar: null },
      created_at: '2026-10-01T15:30:00Z',
    },
  ],
  marathonCompleted: /** @type {number[]} */ ([]),
  timings: /** @type {Record<string, any[]>} */ ({}),
};

const MOCK_USER = {
  id: 'mock-user-1',
  username: 'spywer',
  user_code: '#90360',
  email: 'user@example.com',
  display_name: 'Spywer',
  role: 'user',
  avatar: '/api/profile/avatar/mock-user-1',
  avatar_source: 'internal',
  registered_at: '2026-09-23T00:00:00Z',
  is_private: false,
};

function id() {
  return crypto.randomBytes(8).toString('hex');
}

function json(res, status, body) {
  const payload = body === undefined || body === null ? '' : JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,POST,PATCH,PUT,DELETE,OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, Cookie',
    'Access-Control-Expose-Headers': 'Set-Cookie',
    'Cache-Control': 'no-store',
    'X-Mock-Api': 'yoho-mock',
  });
  res.end(payload);
}

function empty(res, status) {
  res.writeHead(status, {
    'Access-Control-Allow-Origin': '*',
    'Cache-Control': 'no-store',
    'X-Mock-Api': 'yoho-mock',
  });
  res.end();
}

function redirect(res, location) {
  res.writeHead(302, {
    Location: location,
    'Access-Control-Allow-Origin': '*',
  });
  res.end();
}

function notFound(res) {
  json(res, 404, { message: 'Not found', code: 'NOT_FOUND' });
}

function unauthorized(res) {
  json(res, 401, { message: 'Unauthorized', code: 'UNAUTHORIZED' });
}

function parseCookies(req) {
  const header = req.headers.cookie || '';
  const out = {};
  String(header)
    .split(';')
    .map((p) => p.trim())
    .filter(Boolean)
    .forEach((p) => {
      const i = p.indexOf('=');
      if (i < 0) return;
      out[p.slice(0, i).trim()] = decodeURIComponent(p.slice(i + 1).trim());
    });
  return out;
}

function restoreSessionFromCookie(req) {
  const sid = parseCookies(req).yoho_session;
  if (!sid) return false;
  if (auth.token === sid && auth.user) return true;
  // После рестарта mock — восстановить по cookie
  if (String(sid).startsWith('mock-')) {
    auth.token = sid;
    auth.user = { ...MOCK_USER };
    return true;
  }
  return false;
}

function requireAuth(req, res) {
  if (restoreSessionFromCookie(req) || (auth.user && auth.token)) {
    return true;
  }
  unauthorized(res);
  return false;
}

function optionalAuth(req) {
  return restoreSessionFromCookie(req) || Boolean(auth.user);
}

function sessionCookie(token) {
  return `yoho_session=${token}; Path=/; HttpOnly; SameSite=Lax; Max-Age=2592000`;
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8');
      if (!raw) return resolve(null);
      try {
        resolve(JSON.parse(raw));
      } catch (e) {
        reject(e);
      }
    });
    req.on('error', reject);
  });
}

function yearOf(item) {
  return item.year || item.year_start || 0;
}

function scoreOf(item) {
  return item.ratings?.kinopoisk?.score
    || item.ratings?.imdb?.score
    || item.ratings?.tmdb?.score
    || 0;
}

function miniCard(item) {
  if (!item) return null;
  return {
    id_kp: item.id_kp,
    id_tmdb: item.id_tmdb,
    media_type: item.media_type,
    title: item.title,
    year: yearOf(item),
    year_start: item.year_start,
    poster: item.poster,
    ratings: item.ratings,
  };
}

function resolveRelated(ids) {
  const list = Array.isArray(ids) ? ids : [];
  return list
    .map((id) => findById(id))
    .filter(Boolean)
    .map(miniCard)
    .filter(Boolean);
}

function defaultCast() {
  return [
    { id: 1180651, name: 'Майкл Ариас', role: 'Режиссер', poster: '/api/img/kp/actor_posters/kp/1180651.jpg' },
    { id: 2, name: 'Актёр 2', role: 'Второго плана', poster: null },
    { id: 3, name: 'Актёр 3', role: 'Эпизод', poster: null },
  ];
}

const PERSONS = {
  1180651: {
    id: 1180651,
    name: 'Майкл Ариас',
    profession: 'Режиссер, Актер, Монтажер',
    photoUrl: '/api/img/kp/actor_posters/kp/1180651.jpg',
    birthInfo: '2 февраля 1968 · 57 лет',
    birthplace: 'Калифорния, США',
    knownFor: ['Железобетон', 'Аниматрица', 'Годзилла: Минус один'],
  },
};

function asCard(item) {
  const isSeriesLike = item.media_type === 'tv' || item.media_type === 'anime' || item.year_start;
  const related = {
    similar: resolveRelated(item.similar).length
      ? resolveRelated(item.similar)
      : catalog.filter((x) => x.id_kp !== item.id_kp).slice(0, 8).map(miniCard),
    sequels: resolveRelated(item.sequels),
    prequels: resolveRelated(item.prequels),
    remakes: resolveRelated(item.remakes),
  };
  const base = {
    id_kp: item.id_kp,
    id_tmdb: item.id_tmdb,
    media_type: item.media_type,
    title: item.title,
    description: item.description,
    age_rating: item.age_rating,
    ratings: item.ratings,
    genres: item.genres,
    countries: item.countries,
    poster: item.poster,
    backdrop: item.backdrop || item.poster,
    trailer: item.trailer || null,
    cast: item.cast && item.cast.length ? item.cast : defaultCast(),
    cast_total: item.cast_total || (item.cast && item.cast.length) || 3,
    awards_count: item.awards_count || 0,
  };

  if (isSeriesLike && item.media_type !== 'movie' && item.media_type !== 'animation') {
    return {
      ...base,
      media_type: item.media_type === 'anime' ? 'anime' : 'tv',
      year: yearOf(item),
      year_start: item.year_start || item.year,
      year_end: item.year_end ?? null,
      status: item.status || 'SERIAL_ONGOING',
      directors: item.directors || [],
      composer: item.composer,
      totals: item.totals || { seasons: 1, episodes: 1 },
      seasons: item.seasons || defaultSeasons(),
      ...related,
    };
  }

  return {
    ...base,
    media_type: item.media_type === 'animation' ? 'animation' : 'movie',
    year: item.year,
    tagline: item.tagline || null,
    duration: item.duration || { minutes: 120, formatted: '120 мин' },
    director: item.director,
    crew: item.crew || {},
    production: item.production || { distributor: '—' },
    ...related,
  };
}

function defaultSeasons() {
  return [
    {
      number: 1,
      name: '1 сезон',
      episodes_count: 10,
      date_start: '2024-01-01',
      period: 'январь 2024 – март 2024',
      status: 'completed',
      episodes: Array.from({ length: 10 }, (_, i) => ({
        number: i + 1,
        title: `Серия ${i + 1}`,
        title_original: `Episode ${i + 1}`,
        air_date: `2024-01-${String(i + 1).padStart(2, '0')}`,
        air_time: '≈20:00',
        duration: 45,
        description: 'Описание серии (mock).',
        poster: null,
        status: 'released',
      })),
    },
  ];
}

function filterByType(items, type) {
  if (!type || type === 'all') return items;
  if (type === 'movie') return items.filter((i) => i.media_type === 'movie');
  if (type === 'tv') return items.filter((i) => i.media_type === 'tv' || i.media_type === 'anime');
  if (type === 'anime') {
    return items.filter((i) => i.media_type === 'anime' || (i.genres || []).includes('аниме'));
  }
  if (type === 'animation') {
    return items.filter((i) => i.media_type === 'animation' || i.media_type === 'anime');
  }
  return items;
}

function paginate(list, page, limit) {
  const p = Math.max(1, Number(page) || 1);
  const l = Math.min(100, Math.max(1, Number(limit) || 20));
  const start = (p - 1) * l;
  const slice = list.slice(start, start + l);
  return { total: list.length, page: p, limit: l, items: slice, results: slice };
}

function findById(raw) {
  const n = Number(raw);
  return catalog.find((i) => i.id_kp === n || i.id_tmdb === n);
}

function contentKey(type, id) {
  return `${type}:${id}`;
}

function wrapContent(item) {
  return asCard(item);
}

function profilePayload(username) {
  const user = { ...MOCK_USER, username: username || MOCK_USER.username };
  return {
    user: {
      ...user,
      profile_views: 31,
      followers_count: 0,
      following_count: 0,
    },
    stats: {
      yoho_coins: 0,
      timings_count: Object.values(store.timings).flat().length,
      favorites_count: store.favorites.length,
      ratings_count: Object.keys(store.ratings).length,
      votes_count: 0,
    },
    wishlist: { items: store.wishlist, filter: null },
    favorite_films: { slots: 4, filled: Math.min(4, store.favorites.length), items: store.favorites.slice(0, 4) },
    tag_cloud: [
      { name: '2020-е', category: 'years', count: 26, weight: 64 },
      { name: 'драма', category: 'genres', count: 40, weight: 80 },
    ],
    badges: {
      ideas: store.ideas.length,
      dubs: 0,
      reviews: Object.keys(store.ratings).length,
      timings: 0,
      marathon_completed: store.marathonCompleted.length,
      ratings: Object.keys(store.ratings).length,
    },
  };
}

function marathon() {
  const items = catalog.slice(0, 8).map((c, idx) => ({
    position: idx + 1,
    content_id: c.id_kp,
    content_type: c.media_type === 'movie' || c.media_type === 'animation' ? 'movie' : 'tv',
    title: c.title.ru,
    completed: store.marathonCompleted.includes(idx + 1),
    completed_at: store.marathonCompleted.includes(idx + 1) ? new Date().toISOString() : null,
  }));
  return {
    id: 'doomsday',
    title: 'Марафон Doomsday',
    description: 'Mock-марафон для Android',
    deadline: '2026-12-16T00:00:00Z',
    days_left: 42,
    countdown: { days: 42, hours: 7, minutes: 43, seconds: 2 },
    totals: { items: items.length, films: items.filter((i) => i.content_type === 'movie').length, series: items.filter((i) => i.content_type === 'tv').length },
    items,
  };
}

function matchRoute(pathname, pattern) {
  const pp = pattern.split('/').filter(Boolean);
  const ap = pathname.split('/').filter(Boolean);
  if (pp.length !== ap.length) return null;
  const params = {};
  for (let i = 0; i < pp.length; i++) {
    if (pp[i].startsWith('{') && pp[i].endsWith('}')) {
      params[pp[i].slice(1, -1)] = decodeURIComponent(ap[i]);
    } else if (pp[i] !== ap[i]) {
      return null;
    }
  }
  return params;
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') return empty(res, 204);

  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);
  const pathname = url.pathname.replace(/\/+$/, '') || '/';
  const q = url.searchParams;
  const method = req.method || 'GET';

  try {
    let body = null;
    if (method !== 'GET' && method !== 'HEAD' && method !== 'DELETE') {
      body = await readBody(req);
    } else if (method === 'DELETE') {
      // no body usually
    }

    // ---- health ----
    if (method === 'GET' && pathname === '/health') {
      return json(res, 200, { ok: true, items: catalog.length, logged_in: optionalAuth() });
    }

    // ---- auth ----
    if (method === 'POST' && pathname === '/api/auth/sign-in/email') {
      if (!body?.email || !body?.password) {
        return json(res, 400, { message: 'Missing credentials', code: 'BAD_REQUEST' });
      }
      if (!String(body.email).includes('@')) {
        return json(res, 400, { message: 'Invalid email', code: 'INVALID_EMAIL' });
      }
      if (String(body.password).length < 8) {
        return json(res, 401, { message: 'Invalid email or password', code: 'INVALID_EMAIL_OR_PASSWORD' });
      }
      if (String(body.password) === 'wrongpass') {
        return json(res, 401, { message: 'Invalid email or password', code: 'INVALID_EMAIL_OR_PASSWORD' });
      }
      auth.token = 'mock-' + id();
      auth.user = { ...MOCK_USER, email: body.email };
      res.setHeader('Set-Cookie', sessionCookie(auth.token));
      return json(res, 200, {
        user: auth.user,
        session: { token: auth.token, expiresAt: '2027-01-01T00:00:00Z' },
        redirectTo: body.callbackURL || '/',
      });
    }

    if (method === 'POST' && pathname === '/api/auth/sign-up/email') {
      const email = String(body?.email || '').trim();
      const password = String(body?.password || '');
      const username = String(body?.username || '').trim();
      if (!email || !password || !username) {
        return json(res, 400, { message: 'Missing credentials', code: 'BAD_REQUEST' });
      }
      if (!email.includes('@') || !email.includes('.')) {
        return json(res, 400, { message: 'Invalid email', code: 'INVALID_EMAIL' });
      }
      if (password.length < 8) {
        return json(res, 400, { message: 'Password too short', code: 'PASSWORD_TOO_SHORT' });
      }
      if (username.toLowerCase() === 'taken') {
        return json(res, 422, { message: 'Username is already taken', code: 'USERNAME_IS_ALREADY_TAKEN' });
      }
      if (email.toLowerCase() === 'exists@yoho.test') {
        return json(res, 422, { message: 'User already exists', code: 'USER_ALREADY_EXISTS' });
      }
      auth.token = 'mock-' + id();
      auth.user = {
        ...MOCK_USER,
        id: 'mock-user-' + id(),
        email,
        username,
        display_name: body?.name || username || 'User',
      };
      res.setHeader('Set-Cookie', sessionCookie(auth.token));
      return json(res, 200, {
        user: auth.user,
        session: { token: auth.token, expiresAt: '2027-01-01T00:00:00Z' },
        redirectTo: '/',
      });
    }

    {
      const m = matchRoute(pathname, '/api/auth/sign-in/{provider}');
      if (method === 'POST' && m) {
        auth.token = 'mock-oauth-' + id();
        auth.user = { ...MOCK_USER, avatar_source: m.provider };
        res.setHeader('Set-Cookie', sessionCookie(auth.token));
        return json(res, 200, {
          user: auth.user,
          session: { token: auth.token, expiresAt: '2027-01-01T00:00:00Z' },
          redirectTo: body?.callbackURL || '/',
        });
      }
    }

    if (method === 'POST' && pathname === '/api/auth/sign-out') {
      auth.token = null;
      auth.user = null;
      res.setHeader(
        'Set-Cookie',
        'yoho_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax'
      );
      return json(res, 200, { ok: true });
    }

    if (method === 'GET' && pathname === '/api/auth/get-session') {
      restoreSessionFromCookie(req);
      if (!auth.user) return json(res, 200, null);
      return json(res, 200, {
        user: auth.user,
        session: { token: auth.token, expiresAt: '2027-01-01T00:00:00Z' },
      });
    }

    if (method === 'POST' && pathname === '/api/auth/forgot-password') {
      return json(res, 200, { ok: true, message: 'Reset email sent (mock)' });
    }

    if (method === 'POST' && pathname === '/api/auth/reset-password') {
      return json(res, 200, { ok: true });
    }

    if (method === 'GET' && pathname === '/api/auth/verify-email') {
      return json(res, 200, { ok: true });
    }

    // ---- profile ----
    if (method === 'GET' && pathname === '/api/profile/me') {
      if (!requireAuth(req, res)) return;
      return json(res, 200, profilePayload(auth.user.username));
    }

    if (method === 'PATCH' && pathname === '/api/profile/me') {
      if (!requireAuth(req, res)) return;
      auth.user = {
        ...auth.user,
        display_name: body?.display_name || auth.user.display_name,
        is_private: body?.is_private ?? auth.user.is_private,
        username: body?.username || auth.user.username,
      };
      return json(res, 200, profilePayload(auth.user.username));
    }

    {
      let m = matchRoute(pathname, '/api/profile/{username}/films');
      if (method === 'GET' && m) {
        const items = catalog.slice(0, 5).map((c) => ({
          content_id: c.id_kp,
          content_type: c.media_type,
          rating: scoreOf(c),
          content: asCard(c),
        }));
        return json(res, 200, items);
      }
      m = matchRoute(pathname, '/api/profile/{username}/diary');
      if (method === 'GET' && m) return json(res, 200, store.diary);
      m = matchRoute(pathname, '/api/profile/{username}/activity');
      if (method === 'GET' && m) {
        return json(res, 200, store.history.slice(0, 20).map((h) => ({
          id: h.id,
          type: 'watch',
          created_at: h.watched_at,
          content: h.content,
        })));
      }
      m = matchRoute(pathname, '/api/profile/{username}/friends');
      if (method === 'GET' && m) {
        return json(res, 200, { followers: [], following: [] });
      }
      m = matchRoute(pathname, '/api/profile/{username}');
      if (method === 'GET' && m) return json(res, 200, profilePayload(m.username));
    }

    // ---- person ----
    const personMatch = pathname.match(/^\/api\/person\/(\d+)$/);
    if (method === 'GET' && personMatch) {
      const id = Number(personMatch[1]);
      const person = PERSONS[id] || {
        id,
        name: `Персона #${id}`,
        profession: 'Актер',
        photoUrl: `/api/img/kp/actor_posters/kp/${id}.jpg`,
        birthInfo: null,
        birthplace: null,
        knownFor: [],
      };
      return json(res, 200, person);
    }

    // ---- films ----
    if (method === 'GET' && pathname === '/api/film-info') {
      const kpId = Number(q.get('kinopoiskId') || 0) || null;
      const tmdbId = Number(q.get('tmdbId') || 0) || null;
      const mediaType = q.get('mediaType') || 'movie';
      let found = null;
      if (kpId) found = findById(kpId);
      if (!found && tmdbId) {
        found = catalog.find((i) => i.id_tmdb === tmdbId) || null;
      }
      if (!found) return notFound(res);
      const card = asCard(found);
      const kp = found.ratings?.kinopoisk;
      const imdb = found.ratings?.imdb;
      return json(res, 200, {
        details: {
          kinopoiskId: found.id_kp,
          imdbId: null,
          nameRu: found.title?.ru || null,
          nameEn: null,
          nameOriginal: found.title?.original || null,
          posterUrl: found.poster,
          posterUrlPreview: found.poster,
          year: yearOf(found),
          filmLength: found.duration?.minutes || card.duration?.minutes || null,
          slogan: found.tagline || null,
          description: found.description || null,
          shortDescription: null,
          ratingKinopoisk: kp?.score ?? null,
          ratingKinopoiskVoteCount: kp?.votes ?? 0,
          ratingImdb: imdb?.score ?? null,
          ratingImdbVoteCount: imdb?.votes ?? 0,
          ratingAgeLimits: found.age_rating || null,
          countries: (found.countries || []).map((c) => ({ country: c })),
          genres: (found.genres || []).map((g) => ({ genre: g })),
          startYear: found.year_start || null,
          endYear: found.year_end ?? null,
          type: mediaType === 'tv' ? 'TV_SERIES' : 'FILM',
          webUrl: `https://www.kinopoisk.ru/film/${found.id_kp}/`,
        },
        staff: [],
        related: [
          ...(card.prequels || []).map((x) => ({
            filmId: x.id_kp,
            nameRu: x.title?.ru,
            nameOriginal: x.title?.original,
            posterUrl: x.poster,
            relationType: 'PREQUEL',
            mediaType: x.media_type || 'movie',
          })),
          ...(card.sequels || []).map((x) => ({
            filmId: x.id_kp,
            nameRu: x.title?.ru,
            nameOriginal: x.title?.original,
            posterUrl: x.poster,
            relationType: 'SEQUEL',
            mediaType: x.media_type || 'movie',
          })),
        ],
        recommendations: (card.similar || []).map((x) => ({
          id: x.id_tmdb || x.id_kp,
          year: String(x.year || ''),
          title: x.title?.ru || x.title?.original,
          rating: x.ratings?.kinopoisk?.score ?? null,
          mediaType: x.media_type || 'movie',
          posterUrl: x.poster,
          voteCount: x.ratings?.kinopoisk?.votes ?? 0,
          kpId: x.id_kp,
        })),
        backdropUrl: found.backdrop || found.poster,
        resolvedTmdbId: found.id_tmdb || tmdbId,
        resolvedMediaType: found.media_type === 'tv' || found.media_type === 'anime' ? 'tv' : 'movie',
        distributors: found.production?.distributor ? [found.production.distributor] : [],
        rightsHolders: found.production?.rights_holder ? [found.production.rights_holder] : [],
        aboutFilm: [
          { id: 'year', label: 'Год производства', value: String(yearOf(found)) },
          { id: 'country', label: 'Страна', value: (found.countries || []).join(', ') },
          { id: 'genre', label: 'Жанр', value: (found.genres || []).join(', ') },
          found.tagline ? { id: 'slogan', label: 'Слоган', value: found.tagline } : null,
          found.director || (found.directors || [])[0]
            ? (() => {
                const name = found.director || (found.directors || [])[0];
                const pid = found.director_id || 1180651;
                return {
                  id: 'director',
                  label: 'Режиссер',
                  value: name,
                  personIds: { [name]: pid },
                };
              })()
            : null,
          card.duration?.formatted || found.duration?.formatted || found.duration?.minutes
            ? {
                id: 'runtime',
                label: 'Время',
                value: card.duration?.formatted
                  || found.duration?.formatted
                  || `${found.duration?.minutes || card.duration?.minutes} мин`,
              }
            : null,
          found.production?.distributor
            ? { id: 'distributor', label: 'Дистрибьютор', value: found.production.distributor }
            : null,
        ].filter(Boolean),
        mainCast: {
          preview: (card.cast || []).slice(0, 10).map((c, i) => ({
            id: c.id || i + 1,
            name: c.name,
            href: null,
          })),
          total: card.cast_total || (card.cast || []).length,
          castPageHref: null,
        },
        awards: null,
        trailer: found.trailer
          ? {
              title: found.trailer.title,
              youtubeKey: String(found.trailer.url || '').includes('v=')
                ? String(found.trailer.url).split('v=')[1]?.split('&')[0]
                : null,
              publishedAt: found.trailer.date || null,
              thumbnailUrl: found.trailer.thumbnail || found.backdrop || found.poster,
            }
          : null,
        rtRating: null,
        rtUrl: null,
      });
    }

    if (method === 'GET' && pathname === '/api/films') {
      let list = catalog
        .filter((i) => i.media_type === 'movie' || i.media_type === 'animation')
        .map(asCard);
      const genre = q.get('genre');
      const year = q.get('year');
      const sort = q.get('sort') || 'popularity';
      const order = q.get('sort_order') || 'desc';
      if (genre) list = list.filter((i) => (i.genres || []).some((g) => g.toLowerCase().includes(genre.toLowerCase())));
      if (year) list = list.filter((i) => yearOf(i) === Number(year));
      list.sort((a, b) => {
        const av = sort === 'rating' ? scoreOf(a) : sort === 'year' || sort === 'date' ? yearOf(a) : scoreOf(a);
        const bv = sort === 'rating' ? scoreOf(b) : sort === 'year' || sort === 'date' ? yearOf(b) : scoreOf(b);
        return order === 'asc' ? av - bv : bv - av;
      });
      const page = paginate(list, q.get('page'), q.get('limit'));
      return json(res, 200, { total: page.total, page: page.page, limit: page.limit, items: page.items });
    }

    {
      let m = matchRoute(pathname, '/api/films/{id}/timings');
      if (m) {
        const key = contentKey('movie', m.id);
        if (method === 'GET') {
          return json(res, 200, {
            status: (store.timings[key] || []).length ? 'has_content' : 'unknown',
            items: store.timings[key] || [],
          });
        }
        if (method === 'POST') {
          if (!requireAuth(req, res)) return;
          const entry = {
            id: id(),
            timestamp: body?.timestamp || '00:00:00',
            type: body?.type || 'explicit',
            description: body?.description || '',
            spoiler: Boolean(body?.spoiler),
          };
          store.timings[key] = store.timings[key] || [];
          store.timings[key].push(entry);
          return json(res, 201, entry);
        }
      }
      m = matchRoute(pathname, '/api/films/{id}');
      if (method === 'GET' && m) {
        const found = findById(m.id);
        if (!found || (found.media_type !== 'movie' && found.media_type !== 'animation')) return notFound(res);
        return json(res, 200, asCard(found));
      }
    }

    // ---- series ----
    if (method === 'GET' && pathname === '/api/series') {
      let list = catalog
        .filter((i) => i.media_type === 'tv' || i.media_type === 'anime')
        .map(asCard);
      const page = paginate(list, q.get('page'), q.get('limit'));
      return json(res, 200, { total: page.total, page: page.page, limit: page.limit, items: page.items });
    }

    {
      let m = matchRoute(pathname, '/api/series/{id}/seasons/{seasonNumber}');
      if (method === 'GET' && m) {
        const found = findById(m.id);
        if (!found) return notFound(res);
        const card = asCard(found);
        const n = Number(m.seasonNumber);
        const season = (card.seasons || defaultSeasons()).find((s) => s.number === n)
          || { ...defaultSeasons()[0], number: n, name: `${n} сезон` };
        return json(res, 200, season);
      }
      m = matchRoute(pathname, '/api/series/{id}/seasons');
      if (method === 'GET' && m) {
        const found = findById(m.id);
        if (!found) return notFound(res);
        return json(res, 200, asCard(found).seasons || defaultSeasons());
      }
      m = matchRoute(pathname, '/api/series/{id}');
      if (method === 'GET' && m) {
        const found = findById(m.id);
        if (!found || (found.media_type !== 'tv' && found.media_type !== 'anime')) return notFound(res);
        return json(res, 200, asCard(found));
      }
    }

    // ---- search ----
    if (method === 'GET' && pathname === '/api/search/suggest') {
      const query = (q.get('q') || '').trim().toLowerCase();
      const limit = Math.min(20, Number(q.get('limit') || 10));
      if (!query) return json(res, 200, []);
      const results = catalog
        .filter((i) => {
          const ru = (i.title?.ru || '').toLowerCase();
          const or = (i.title?.original || '').toLowerCase();
          return ru.includes(query) || or.includes(query);
        })
        .slice(0, limit)
        .map((i) => ({
          id: i.id_kp,
          type: i.media_type,
          title: i.title.ru,
          year: yearOf(i),
          poster: i.poster,
        }));
      return json(res, 200, results);
    }

    if (method === 'GET' && pathname === '/api/search/facets') {
      return json(res, 200, {
        types: [
          { value: 'all', label: 'Все', count: catalog.length, available: true },
          { value: 'movie', label: 'Фильмы', count: catalog.filter((i) => i.media_type === 'movie').length, available: true },
          { value: 'tv', label: 'Сериалы', count: catalog.filter((i) => i.media_type === 'tv').length, available: true },
          { value: 'anime', label: 'Аниме', count: catalog.filter((i) => i.media_type === 'anime').length, available: true },
          { value: 'animation', label: 'Мультфильмы', count: catalog.filter((i) => i.media_type === 'animation').length, available: true },
        ],
        genres: [
          { value: 'action', label: 'Боевик', count: 3, available: true },
          { value: 'biography', label: 'Биография', count: 1, available: true },
          { value: 'comedy', label: 'Комедия', count: 2, available: true },
          { value: 'crime', label: 'Криминал', count: 2, available: true },
          { value: 'drama', label: 'Драма', count: 5, available: true },
          { value: 'fantasy', label: 'Фэнтези', count: 2, available: true },
          { value: 'horror', label: 'Ужасы', count: 2, available: true },
          { value: 'mystery', label: 'Детектив', count: 1, available: true },
          { value: 'romance', label: 'Мелодрама', count: 1, available: true },
          { value: 'speculative_fiction', label: 'Фантастика', count: 3, available: true },
          { value: 'thriller', label: 'Триллер', count: 2, available: true },
          { value: 'war', label: 'Военный', count: 1, available: true },
        ],
        countries: [
          { value: 'US', label: 'США', count: 7, available: true },
          { value: 'GB', label: 'Великобритания', count: 2, available: true },
          { value: 'RU', label: 'Россия', count: 1, available: true },
          { value: 'JP', label: 'Япония', count: 2, available: true },
          { value: 'KR', label: 'Корея', count: 1, available: true },
        ],
        regions: [
          { value: 'cis', label: 'СНГ', count: 1, available: true },
          { value: 'europe', label: 'Европа', count: 2, available: true },
          { value: 'america', label: 'Америка', count: 7, available: true },
          { value: 'asia', label: 'Азия', count: 3, available: true },
        ],
        years: { min: 1994, max: 2026 },
      });
    }

    if (method === 'GET' && pathname === '/api/search/regions') {
      return json(res, 200, {
        regions: [
          { slug: 'cis', label: 'СНГ', countries: ['RU', 'BY', 'KZ', 'UA'], countries_count: 4 },
          { slug: 'europe', label: 'Европа', countries: ['GB', 'DE', 'FR', 'IT'], countries_count: 4 },
          { slug: 'asia', label: 'Азия', countries: ['JP', 'KR', 'CN', 'IN'], countries_count: 4 },
          { slug: 'america', label: 'Америка', countries: ['US', 'CA', 'MX', 'BR'], countries_count: 4 },
          { slug: 'africa', label: 'Африка', countries: ['EG', 'ZA', 'NG'], countries_count: 3 },
          { slug: 'oceania', label: 'Океания', countries: ['AU', 'NZ'], countries_count: 2 },
        ],
      });
    }

    if (method === 'GET' && pathname === '/api/search/filters') {
      return json(res, 200, {
        kinds: {
          movie: catalog.filter((i) => i.media_type === 'movie').length,
          short: 0,
          series: catalog.filter((i) => i.media_type === 'tv').length,
          cartoon: catalog.filter((i) => i.media_type === 'animation').length,
          tv_show: 0,
          anime: catalog.filter((i) => i.media_type === 'anime').length,
          mini_series: 0,
        },
        tags: [
          { tag: 'По книге', kind: 'keyword' },
          { tag: 'Основано на реальных событиях', kind: 'keyword' },
          { tag: 'Мрачный', kind: 'adjective' },
          { tag: 'Напряжённый', kind: 'adjective' },
          { tag: 'Эпический', kind: 'adjective' },
          { tag: 'Супергерои', kind: 'subgenre' },
          { tag: 'Киберпанк', kind: 'subgenre' },
          { tag: 'Исекай', kind: 'subgenre' },
          { tag: 'Чёрно-белый', kind: 'subgenre' },
          { tag: 'Дружба', kind: 'keyword' },
          { tag: 'Месть', kind: 'keyword' },
          { tag: 'Путешествие во времени', kind: 'keyword' },
        ],
        platforms: [
          { slug: 'netflix', name: 'Netflix' },
          { slug: 'kinopoisk', name: 'Кинопоиск HD' },
          { slug: 'ivi', name: 'IVI' },
          { slug: 'okko', name: 'Okko' },
          { slug: 'premier', name: 'Premier' },
          { slug: 'start', name: 'START' },
          { slug: 'wink', name: 'Wink' },
          { slug: 'disney', name: 'Disney+' },
          { slug: 'hbo_max', name: 'Max' },
          { slug: 'prime', name: 'Prime Video' },
          { slug: 'apple_tv', name: 'Apple TV+' },
        ],
        companies: [
          'Warner Bros.', 'Universal', 'Sony', 'Paramount', 'Disney',
          'DreamWorks', 'A24', 'Netflix Studios', 'BBC', 'HBO', 'Pixar', 'Ghibli',
        ],
      });
    }

    if (method === 'GET' && pathname === '/api/search/recent') {
      const items = catalog.slice(0, 5).map((i) => ({
        id: i.id_kp,
        media_type: i.media_type,
        title: i.title.ru,
        year: yearOf(i),
        poster: i.poster,
        added_at: new Date().toISOString(),
      }));
      return json(res, 200, { items, total: items.length });
    }

    if (method === 'GET' && pathname === '/api/search/discover') {
      const limit = Math.min(50, Number(q.get('limit') || 24));
      const source = q.get('source') || 'catalog';
      let list = [...catalog];
      if (source === 'trending') list = list.filter((i) => (i.tags || []).includes('now'));
      if (source === 'popular') list = list.filter((i) => (i.tags || []).includes('popular'));

      const typeCsv = (q.get('type') || '').split(',').filter(Boolean);
      if (typeCsv.length) {
        list = list.filter((i) => {
          const map = {
            movie: i.media_type === 'movie',
            series: i.media_type === 'tv',
            anime: i.media_type === 'anime',
            cartoon: i.media_type === 'animation',
            animation: i.media_type === 'animation',
            tv: i.media_type === 'tv',
            mini_series: false,
            tv_show: i.media_type === 'tv',
            short: false,
          };
          return typeCsv.some((t) => map[t]);
        });
      }

      const query = (q.get('q') || q.get('title') || '').trim().toLowerCase();
      if (query) {
        list = list.filter((i) => {
          const ru = (i.title?.ru || '').toLowerCase();
          const or = (i.title?.original || '').toLowerCase();
          return ru.includes(query) || or.includes(query);
        });
      }

      const genreMap = {
        action: 'боевик', adventure: 'приключения', biography: 'биография', comedy: 'комедия',
        crime: 'криминал', documentary: 'документальный', drama: 'драма', family: 'семейный',
        fantasy: 'фэнтези', horror: 'ужасы', mystery: 'детектив', mysticism: 'мистика',
        romance: 'мелодрама', speculative_fiction: 'фантастика', thriller: 'триллер',
        war: 'военный', western: 'вестерн', history: 'история', sport: 'спорт',
      };
      const genres = (q.get('genres') || '').split(',').filter(Boolean);
      if (genres.length) {
        list = list.filter((i) =>
          genres.some((g) => {
            const ru = genreMap[g] || g;
            return (i.genres || []).some((x) => {
              const s = String(x).toLowerCase();
              return s.includes(g.toLowerCase()) || s.includes(String(ru).toLowerCase());
            });
          })
        );
      }

      const countries = (q.get('country') || '').split(',').filter(Boolean);
      if (countries.length) {
        list = list.filter((i) =>
          countries.some((c) => (i.countries || []).some((x) => String(x).toUpperCase().includes(c.toUpperCase())
            || (c === 'US' && String(x).includes('США'))
            || (c === 'JP' && String(x).includes('Япон'))
            || (c === 'RU' && String(x).includes('Росс'))
            || (c === 'GB' && String(x).includes('Британ'))
            || (c === 'KR' && String(x).includes('Коре')))
          )
        );
      }

      const yearFrom = q.get('year_from');
      const yearTo = q.get('year_to');
      if (yearFrom) list = list.filter((i) => yearOf(i) >= Number(yearFrom));
      if (yearTo) list = list.filter((i) => yearOf(i) <= Number(yearTo));

      const ratingMin = q.get('rating_kp_min') || q.get('rating_min');
      const ratingMax = q.get('rating_kp_max') || q.get('rating_max');
      if (ratingMin) list = list.filter((i) => scoreOf(i) >= Number(ratingMin));
      if (ratingMax) list = list.filter((i) => scoreOf(i) <= Number(ratingMax));

      const sort = q.get('sort') || 'popularity';
      list = [...list].sort((a, b) => {
        const av = sort === 'rating' ? scoreOf(a) : sort === 'year' ? yearOf(a) : scoreOf(a);
        const bv = sort === 'rating' ? scoreOf(b) : sort === 'year' ? yearOf(b) : scoreOf(b);
        return bv - av;
      });

      const total = list.length;
      const page = Math.max(1, Number(q.get('page') || 1));
      const totalPages = Math.max(1, Math.ceil(total / limit) || 1);
      const start = (page - 1) * limit;
      const sliced = list.slice(start, start + limit);
      const results = sliced.map((i) => ({
        tmdbId: i.id_tmdb || null,
        kinopoiskId: i.id_kp,
        mediaType: i.media_type,
        title: i.title.ru,
        originalTitle: i.title.original,
        year: String(yearOf(i) || ''),
        posterUrl: i.poster,
        rating: scoreOf(i),
        genres: i.genres,
        ratings: {
          kp: i.ratings?.kinopoisk?.score ?? null,
          imdb: i.ratings?.imdb?.score ?? null,
        },
      }));
      return json(res, 200, {
        results,
        page,
        totalPages,
        hasMore: page < totalPages,
        total,
        source,
      });
    }

    if (method === 'GET' && pathname === '/api/film/watchers') {
      return json(res, 200, {
        watchers: [
          {
            id: 'mock-user-1',
            code: '#90360',
            name: 'Spywer',
            avatar: '/api/profile/avatar/mock-user-1',
            isStreamer: false,
            isLive: false,
            score: 9,
            rating: 'like',
            subscribed: false,
          },
        ],
      });
    }

    if (method === 'GET' && pathname === '/api/tv-schedule') {
      const tmdbId = Number(q.get('tmdbId'));
      const found = catalog.find((c) => c.id_tmdb === tmdbId) || findById(q.get('kinopoiskId'));
      const seasons = (found ? asCard(found).seasons : defaultSeasons()) || defaultSeasons();
      const payload = {
        tmdbId: tmdbId || found?.id_tmdb || 0,
        seasons: seasons.map((s) => ({
          seasonNumber: s.number,
          episodes: (s.episodes || []).map((e) => ({
            seasonNumber: s.number,
            episodeNumber: e.number,
            title: e.title,
            airDate: e.air_date,
          })).reverse(),
        })).reverse(),
      };
      if (q.get('release') === '1') {
        const dubs = {};
        (payload.seasons || []).forEach((s) => {
          (s.episodes || []).slice(0, 3).forEach((e) => {
            dubs[`${s.seasonNumber}:${e.episodeNumber}`] = [
              { name: 'ColdFilm', at: Date.now() },
              { name: 'Dragon Money Studio', at: Date.now() },
              { name: 'LostFilm', at: Date.now() },
              { name: 'HDrezka Studio', at: Date.now() },
              { name: 'TVShows', at: Date.now() },
            ];
          });
        });
        payload.release = {
          context: {
            networks: ['HBO'],
            originCountry: found?.countries || ['US'],
            airTimes: {},
            shikimoriNextAt: null,
          },
          dubs,
          journal: { since: Date.now() - 86400000, updatedAt: Date.now() },
        };
      }
      return json(res, 200, payload);
    }

    if (method === 'GET' && pathname === '/api/tv-last-air') {
      const tmdbId = Number(q.get('tmdbId'));
      const found = catalog.find((c) => c.id_tmdb === tmdbId);
      const seasons = found?.totals?.seasons || 1;
      const episodes = found?.totals?.episodes || 1;
      const ep = Math.min(5, episodes);
      const fp = `${seasons}|${ep}|2026-11-01`;
      return json(res, 200, {
        fp,
        badge: found?.status === 'SERIAL_COMPLETE' ? 'СЕРИАЛ ЗАВЕРШЁН' : `${seasons} СЕЗОН ${ep} СЕРИЯ`,
        meta: {
          tmdbId,
          seasonNumber: seasons,
          episodeNumber: ep,
          airDate: '2026-11-01',
          fingerprint: fp,
          showStatus: found?.status === 'SERIAL_COMPLETE' ? 'Ended' : 'Returning Series',
          inProduction: found?.status !== 'SERIAL_COMPLETE',
          airAt: Date.parse('2026-11-01T20:00:00Z'),
          airPrecision: 'day',
          nextSeasonNumber: found?.status === 'SERIAL_COMPLETE' ? null : seasons,
          nextEpisodeNumber: found?.status === 'SERIAL_COMPLETE' ? null : ep + 1,
          nextAirDate: found?.status === 'SERIAL_COMPLETE' ? null : '2026-11-08',
          nextAirAt: found?.status === 'SERIAL_COMPLETE' ? null : Date.parse('2026-11-08T20:00:00Z'),
          nextAirPrecision: 'day',
        },
      });
    }

    if (method === 'GET' && pathname === '/api/ratings') {
      if (!requireAuth(req, res)) return;
      return json(res, 200, {
        ratings: Object.entries(store.ratings).map(([key, r]) => ({
          id: key,
          ...r,
        })),
      });
    }

    if (method === 'GET' && pathname === '/api/player-choice') {
      const itemId = q.get('itemId') || '';
      if (!/^(kp|tmdb|imdb):\d+$/.test(itemId)) {
        return json(res, 400, { message: 'Invalid itemId', code: 'BAD_REQUEST' });
      }
      return json(res, 200, {
        total: 226,
        items: [
          { source: 'alloha', count: 126, share: 0.5575 },
          { source: 'turbo', count: 62, share: 0.2743 },
          { source: 'collaps', count: 20, share: 0.0885 },
          { source: 'cvh', count: 15, share: 0.0664 },
          { source: 'vibix', count: 3, share: 0.0133 },
          { source: 'videoseed', count: 0, share: 0 },
          { source: 'veoveo', count: 0, share: 0 },
        ],
      });
    }

    if (method === 'GET' && pathname === '/api/seen-film') {
      return json(res, 200, {
        total: 1000,
        items: [
          { source: 'Demo', count: 400, share: 0.4 },
          { source: 'Alloha', count: 350, share: 0.35 },
          { source: 'Kodik', count: 250, share: 0.25 },
        ],
      });
    }

    if (method === 'GET' && pathname === '/api/og/title') {
      const kind = q.get('kind') || 'film';
      const idVal = q.get('id') || '0';
      return json(res, 200, {
        kind,
        id: idVal,
        title: `OG mock ${kind}/${idVal}`,
        image: 'https://image.tmdb.org/t/p/w780/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg',
      });
    }

    if (method === 'GET' && pathname.match(/^\/api\/img\/kp\/posters\/kp_small\/\d+\.jpg$/)) {
      const gif = Buffer.from(
        'R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
        'base64'
      );
      res.writeHead(200, { 'Content-Type': 'image/gif', 'Cache-Control': 'public, max-age=3600' });
      return res.end(gif);
    }

    if (method === 'GET' && pathname === '/api/search') {
      const query = (q.get('q') || '').trim().toLowerCase();
      let list = filterByType(catalog, q.get('type') || 'all');
      if (query) {
        list = list.filter((i) => {
          const ru = (i.title?.ru || '').toLowerCase();
          const or = (i.title?.original || '').toLowerCase();
          return ru.includes(query) || or.includes(query);
        });
      }
      const genreMap = {
        action: 'боевик', adventure: 'приключения', biography: 'биография', comedy: 'комедия',
        crime: 'криминал', documentary: 'документальный', drama: 'драма', family: 'семейный',
        fantasy: 'фэнтези', horror: 'ужасы', mystery: 'детектив', mysticism: 'мистика',
        romance: 'мелодрама', speculative_fiction: 'фантастика', thriller: 'триллер',
        war: 'военный', western: 'вестерн', history: 'история', sport: 'спорт',
      };
      const genres = (q.get('genres') || '').split(',').filter(Boolean);
      if (genres.length) {
        list = list.filter((i) =>
          genres.some((g) => {
            const ru = genreMap[g] || g;
            return (i.genres || []).some((x) => {
              const s = String(x).toLowerCase();
              return s.includes(g.toLowerCase()) || s.includes(String(ru).toLowerCase());
            });
          })
        );
      }
      const countries = (q.get('country') || '').split(',').filter(Boolean);
      if (countries.length) {
        list = list.filter((i) =>
          countries.some((c) => (i.countries || []).some((x) => String(x).toUpperCase().includes(c.toUpperCase())
            || (c === 'US' && String(x).includes('США'))
            || (c === 'JP' && String(x).includes('Япон'))
            || (c === 'RU' && String(x).includes('Росс'))
            || (c === 'GB' && String(x).includes('Британ'))
            || (c === 'KR' && String(x).includes('Коре')))
          )
        );
      }
      const yearFrom = q.get('year_from');
      const yearTo = q.get('year_to');
      if (yearFrom) list = list.filter((i) => yearOf(i) >= Number(yearFrom));
      if (yearTo) list = list.filter((i) => yearOf(i) <= Number(yearTo));
      const ratingMin = q.get('rating_min');
      if (ratingMin) list = list.filter((i) => scoreOf(i) >= Number(ratingMin));
      const sort = q.get('sort') || 'popularity';
      const order = q.get('sort_order') || 'desc';
      list = [...list].sort((a, b) => {
        const av = sort === 'rating' ? scoreOf(a) : sort === 'year' ? yearOf(a) : scoreOf(a);
        const bv = sort === 'rating' ? scoreOf(b) : sort === 'year' ? yearOf(b) : scoreOf(b);
        return order === 'asc' ? av - bv : bv - av;
      });
      const page = paginate(list.map(asCard), q.get('page'), q.get('limit'));
      return json(res, 200, {
        total: page.total,
        page: page.page,
        limit: page.limit,
        results: page.results,
      });
    }

    if (method === 'POST' && pathname === '/api/getlucky') {
      const pool = filterByType(catalog, body?.type || 'all').map(asCard);
      const film = pool[Math.floor(Math.random() * pool.length)] || asCard(catalog[0]);
      return json(res, 200, {
        film,
        roulette_items: pool.slice(0, 32).map((i) => ({
          id: i.id_kp,
          title: i.title.ru,
          year: yearOf(i),
          poster: i.poster,
        })),
        sound_enabled: true,
      });
    }

    // ---- collections ----
    if (method === 'GET' && pathname === '/api/collections/counters') {
      return json(res, 200, {
        movies: {
          best: 14823,
          genres: { comedy: 48842, drama: 79748, thriller: 23525, action: 19236 },
          countries: { russia: 7389, usa: 67816, japan: 7684 },
        },
        series: {
          best: 10380,
          genres: { drama: 12996, comedy: 9488, anime: 2997 },
        },
      });
    }

    if (method === 'GET' && pathname === '/api/collections/movies') {
      const list = catalog.filter((i) => i.media_type === 'movie' || i.media_type === 'animation').map(asCard);
      const page = paginate(list, q.get('page'), q.get('limit'));
      return json(res, 200, { total: page.total, page: page.page, limit: page.limit, items: page.items });
    }

    if (method === 'GET' && pathname === '/api/collections/tv') {
      const list = catalog.filter((i) => i.media_type === 'tv' || i.media_type === 'anime').map(asCard);
      const page = paginate(list, q.get('page'), q.get('limit'));
      return json(res, 200, { total: page.total, page: page.page, limit: page.limit, items: page.items });
    }

    if (method === 'GET' && pathname === '/api/collections/franchises') {
      return json(res, 200, [
        {
          id: 'matrix',
          title: 'Матрица',
          items_count: 4,
          poster: catalog.find((c) => c.id_kp === 301)?.poster,
          chronology_url: '/franchises/matrix',
        },
        {
          id: 'marvel',
          title: 'Marvel',
          items_count: 69,
          poster: catalog[0]?.poster,
          chronology_url: '/marvel',
        },
      ]);
    }

    if (method === 'GET' && pathname === '/api/collections/marvel') {
      return json(res, 200, {
        title: 'Фильмы Marvel по порядку',
        total: catalog.length,
        items: catalog.map((c, idx) => ({
          position: idx + 1,
          content_id: c.id_kp,
          content_type: c.media_type === 'tv' || c.media_type === 'anime' ? 'tv' : 'movie',
          title: c.title.ru,
          year: yearOf(c),
          poster: c.poster,
        })),
      });
    }

    if (method === 'GET' && pathname === '/api/collections/anime-top') {
      const list = catalog
        .filter((i) => i.media_type === 'anime' || (i.genres || []).includes('аниме'))
        .map(asCard);
      return json(res, 200, list);
    }

    // ---- comments / rate  /api/{type}/{id}/... ----
    {
      let m = matchRoute(pathname, '/api/{type}/{id}/comments');
      if (m && (m.type === 'movie' || m.type === 'tv' || m.type === 'anime' || m.type === 'animation' || m.type === 'film' || m.type === 'series')) {
        const key = contentKey(m.type, m.id);
        if (method === 'GET') {
          const comments = store.comments[key] || [];
          const page = paginate(comments, q.get('page'), q.get('limit') || 20);
          return json(res, 200, { comments: page.items, total: page.total, page: page.page });
        }
        if (method === 'POST') {
          if (!requireAuth(req, res)) return;
          const comment = {
            id: id(),
            user: { id: auth.user.id, username: auth.user.username, avatar: auth.user.avatar },
            text: body?.text || '',
            created_at: new Date().toISOString(),
            edited: false,
            has_spoilers: Boolean(body?.spoiler),
            reactions: { laugh: 0, poop: 0, clown: 0, fire: 0, heart: 0, total: 0 },
            replies: [],
            parent_id: body?.parent_id || null,
          };
          store.comments[key] = store.comments[key] || [];
          store.comments[key].unshift(comment);
          return json(res, 201, comment);
        }
      }

      m = matchRoute(pathname, '/api/{type}/{id}/rate');
      if (m && (m.type === 'movie' || m.type === 'tv' || m.type === 'anime' || m.type === 'animation' || m.type === 'film' || m.type === 'series')) {
        const key = contentKey(m.type, m.id);
        if (method === 'POST') {
          if (!requireAuth(req, res)) return;
          store.ratings[key] = {
            content_id: Number(m.id),
            content_type: m.type,
            rating: body?.rating,
            review: body?.review || '',
            spoiler: Boolean(body?.spoiler),
            updated_at: new Date().toISOString(),
          };
          return json(res, 200, store.ratings[key]);
        }
        if (method === 'DELETE') {
          if (!requireAuth(req, res)) return;
          delete store.ratings[key];
          return empty(res, 204);
        }
      }
    }

    {
      const m = matchRoute(pathname, '/api/comments/{id}/react');
      if (method === 'POST' && m) {
        if (!requireAuth(req, res)) return;
        for (const list of Object.values(store.comments)) {
          const c = list.find((x) => x.id === m.id);
          if (c) {
            const r = body?.reaction;
            if (r && c.reactions[r] !== undefined) {
              if (body?.remove) c.reactions[r] = Math.max(0, c.reactions[r] - 1);
              else c.reactions[r] += 1;
              c.reactions.total = ['laugh', 'poop', 'clown', 'fire', 'heart']
                .reduce((s, k) => s + c.reactions[k], 0);
            }
            return json(res, 200, c);
          }
        }
        return notFound(res);
      }
    }

    // ---- diary ----
    if (pathname === '/api/diary') {
      if (method === 'GET') {
        if (!requireAuth(req, res)) return;
        let list = store.diary;
        const status = q.get('status');
        if (status) list = list.filter((d) => d.status === status);
        return json(res, 200, list);
      }
      if (method === 'POST') {
        if (!requireAuth(req, res)) return;
        const found = findById(body?.content_id);
        const entry = {
          id: id(),
          content_id: body?.content_id,
          content_type: body?.content_type,
          status: body?.status || 'want_to_watch',
          rating: body?.rating ?? null,
          note: body?.note || '',
          added_at: new Date().toISOString(),
          progress: { season: null, episode: null, percentage: 0 },
          content: found ? wrapContent(found) : null,
        };
        store.diary.unshift(entry);
        return json(res, 201, entry);
      }
    }

    {
      const m = matchRoute(pathname, '/api/diary/{id}');
      if (m) {
        const idx = store.diary.findIndex((d) => d.id === m.id);
        if (method === 'PATCH') {
          if (!requireAuth(req, res)) return;
          if (idx < 0) return notFound(res);
          store.diary[idx] = { ...store.diary[idx], ...body, id: m.id };
          return json(res, 200, store.diary[idx]);
        }
        if (method === 'DELETE') {
          if (!requireAuth(req, res)) return;
          if (idx < 0) return notFound(res);
          store.diary.splice(idx, 1);
          return empty(res, 204);
        }
      }
    }

    // ---- me/activity (addFavorite / removeFavorite) ----
    if (method === 'POST' && pathname === '/api/me/activity') {
      const op = body?.op;
      const item = body?.item;
      if (!item || !item.id) return json(res, 400, { message: 'item required', code: 'BAD_REQUEST' });
      if (op === 'addFavorite') {
        const exists = store.favorites.some((f) => f.id === item.id || `kp:${f.content_id}` === item.id);
        if (!exists) {
          const kp = Number(String(item.id).replace(/^kp:/, '')) || null;
          store.favorites.unshift({
            id: item.id,
            content_id: kp,
            content_type: item.mediaType || 'movie',
            href: item.href,
            title: item.title,
            year: item.year,
            posterUrl: item.posterUrl,
            mediaType: item.mediaType,
            added_at: new Date().toISOString(),
            ts: item.ts || Date.now(),
          });
        }
        return empty(res, 204);
      }
      if (op === 'removeFavorite') {
        store.favorites = store.favorites.filter(
          (f) => f.id !== item.id && `kp:${f.content_id}` !== item.id,
        );
        return empty(res, 204);
      }
      return json(res, 400, { message: `Unknown op: ${op}`, code: 'BAD_REQUEST' });
    }

    // ---- favorites ----
    if (pathname === '/api/favorites') {
      if (method === 'GET') {
        // mock: без логина тоже отдаём список, чтобы UI мог сверить состояние
        return json(res, 200, store.favorites);
      }
      if (method === 'POST') {
        if (!requireAuth(req, res)) return;
        const exists = store.favorites.some(
          (f) => f.content_id === body?.content_id && f.content_type === body?.content_type
        );
        if (exists) return json(res, 409, { message: 'Already exists', code: 'CONFLICT' });
        const found = findById(body?.content_id);
        const item = {
          id: id(),
          content_id: body?.content_id,
          content_type: body?.content_type,
          added_at: new Date().toISOString(),
          content: found ? wrapContent(found) : null,
        };
        store.favorites.unshift(item);
        return json(res, 201, item);
      }
    }

    {
      const m = matchRoute(pathname, '/api/favorites/{content_type}/{content_id}');
      if (method === 'DELETE' && m) {
        if (!requireAuth(req, res)) return;
        const before = store.favorites.length;
        store.favorites = store.favorites.filter(
          (f) => !(String(f.content_id) === m.content_id && f.content_type === m.content_type)
        );
        if (store.favorites.length === before) return notFound(res);
        return empty(res, 204);
      }
    }

    // ---- wishlist ----
    if (pathname === '/api/wishlist') {
      if (method === 'GET') {
        if (!requireAuth(req, res)) return;
        return json(res, 200, store.wishlist);
      }
      if (method === 'POST') {
        if (!requireAuth(req, res)) return;
        const found = findById(body?.content_id);
        const item = {
          id: id(),
          content_id: body?.content_id,
          content_type: body?.content_type,
          added_at: new Date().toISOString(),
          content: found ? wrapContent(found) : null,
        };
        store.wishlist.unshift(item);
        return json(res, 201, item);
      }
    }

    // ---- history ----
    if (pathname === '/api/history') {
      if (method === 'GET') {
        if (!requireAuth(req, res)) return;
        const page = paginate(store.history, q.get('page'), q.get('limit'));
        return json(res, 200, page.items);
      }
      if (method === 'POST') {
        if (!requireAuth(req, res)) return;
        const found = findById(body?.content_id);
        const item = {
          id: id(),
          content_id: body?.content_id,
          content_type: body?.content_type,
          watched_at: new Date().toISOString(),
          progress: body?.progress || { percentage: 0 },
          content: found ? wrapContent(found) : null,
        };
        store.history.unshift(item);
        return json(res, 201, item);
      }
    }

    // ---- marathon ----
    if (method === 'GET' && pathname === '/api/marathon') {
      return json(res, 200, marathon());
    }

    if (method === 'GET' && pathname === '/api/marathon/progress') {
      if (!requireAuth(req, res)) return;
      const completed = store.marathonCompleted.slice().sort((a, b) => a - b);
      return json(res, 200, {
        completed_count: completed.length,
        current_position: completed.length ? completed[completed.length - 1] + 1 : 1,
        completed_items: completed,
      });
    }

    {
      const m = matchRoute(pathname, '/api/marathon/items/{position}/complete');
      if (method === 'POST' && m) {
        if (!requireAuth(req, res)) return;
        const pos = Number(m.position);
        if (!store.marathonCompleted.includes(pos)) store.marathonCompleted.push(pos);
        return json(res, 200, { ok: true, position: pos });
      }
    }

    // ---- calendar ----
    if (method === 'GET' && pathname === '/api/calendar') {
      const items = catalog
        .filter((i) => i.media_type === 'tv' || i.media_type === 'anime')
        .map((i, idx) => ({
          series_id: i.id_kp,
          title: i.title.ru,
          season: 1,
          episode: idx + 1,
          air_date: `2026-11-${String((idx % 28) + 1).padStart(2, '0')}`,
          poster: i.poster,
        }));
      return json(res, 200, items);
    }

    // ---- player (stub sources only — no third-party stream tokens) ----
    {
      const m = matchRoute(pathname, '/api/player/{type}/{id}');
      if (method === 'GET' && m) {
        const found = findById(m.id);
        return json(res, 200, {
          players: [
            {
              name: 'alloha',
              quality: '1080p',
              active: true,
              share: 55,
              dub: 'Оригинал',
              iframe_url: found?.trailer?.url || 'https://www.youtube.com/embed/dQw4w9WgXcQ',
              tokens: null,
              uses_storage_access: false,
              provider_number: 0,
            },
            {
              name: 'turbo',
              quality: '1080p',
              active: false,
              share: 27,
              dub: 'Оригинал',
              iframe_url: 'https://www.youtube.com/embed/aqz-KE-bpKQ?autoplay=0&rel=0',
              tokens: null,
              uses_storage_access: false,
              provider_number: 1,
            },
            {
              name: 'collaps',
              quality: '720p',
              active: false,
              share: 9,
              dub: 'Оригинал',
              iframe_url: 'https://www.youtube.com/embed/LXb3EKWsInQ?autoplay=0&rel=0',
              tokens: null,
              uses_storage_access: false,
              provider_number: 2,
            },
          ],
          quality_tags: ['good_quality', 'no_ads'],
          timings: {
            status: 'unknown',
            items: store.timings[contentKey(m.type, m.id)] || [],
          },
        });
      }
    }

    // ---- images: redirect to TMDB / placeholder ----
    {
      const m = matchRoute(pathname, '/api/img/t/p/{size}/{path}');
      if (method === 'GET' && m) {
        return redirect(res, `https://image.tmdb.org/t/p/${m.size}/${m.path}`);
      }
      // path may contain slashes — handle prefix
      if (method === 'GET' && pathname.startsWith('/api/img/t/p/')) {
        const rest = pathname.slice('/api/img/t/p/'.length);
        return redirect(res, `https://image.tmdb.org/t/p/${rest}`);
      }
      if (method === 'GET' && pathname.startsWith('/api/img/kp-mds/')) {
        // no KP proxy in mock — 1x1 gif
        const gif = Buffer.from(
          'R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
          'base64'
        );
        res.writeHead(200, { 'Content-Type': 'image/gif', 'Cache-Control': 'public, max-age=3600' });
        return res.end(gif);
      }
      const av = matchRoute(pathname, '/api/profile/avatar/{id}');
      if (method === 'GET' && av) {
        const gif = Buffer.from(
          'R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
          'base64'
        );
        res.writeHead(200, { 'Content-Type': 'image/gif', 'Cache-Control': 'public, max-age=3600' });
        return res.end(gif);
      }
    }

    // ---- recommendations (prod: /api/recommendations/latest) ----
    if (method === 'GET' && pathname === '/api/recommendations/latest') {
      if (!optionalAuth(req)) {
        return json(res, 401, { message: 'Unauthorized', code: 'UNAUTHORIZED' });
      }
      const items = catalog.slice(0, 12).map((i) => ({
        title: i.title?.ru || i.title?.original || 'Title',
        year: String(i.year || ''),
        mediaType: i.media_type || 'movie',
        posterUrl: i.poster,
        href: i.media_type === 'tv' || i.media_type === 'anime'
          ? `/series/${i.id_kp}`
          : `/film/${i.id_kp}`,
        itemId: i.id_tmdb ? `tmdb:${i.id_tmdb}` : `kp:${i.id_kp}`,
        tmdbId: i.id_tmdb || null,
        rating: i.ratings?.kinopoisk?.score ?? null,
        reason: 'Популярное в ваших жанрах',
        confidence: 0.75,
        sourceTitle: 'По вашим жанрам',
        sourceTmdbId: null,
        genreIds: [],
      }));
      return json(res, 200, {
        items,
        cachedAt: new Date().toISOString(),
        profileHash: 'mock',
      });
    }

    // ---- trending / trailers (docs v0.1.40+) ----
    if (method === 'GET' && pathname === '/api/trending') {
      const filter = q.get('filter') || '';
      let list = catalog;
      if (filter === 'popular') {
        list = catalog.filter((i) => (i.tags || []).includes('popular'));
      } else if (filter === 'movie' || filter === 'popular_movie') {
        list = catalog.filter((i) => i.media_type === 'movie');
      } else if (filter === 'tv' || filter === 'popular_tv') {
        list = catalog.filter((i) => i.media_type === 'tv' || i.media_type === 'anime');
      } else if (filter === 'animation' || filter === 'popular_animation') {
        list = catalog.filter((i) => i.media_type === 'animation' || i.media_type === 'anime');
      } else {
        list = catalog.filter((i) => (i.tags || []).includes('now') || (i.tags || []).includes('popular'));
      }
      if (filter === 'popular' || filter.startsWith('popular_')) {
        list = [...list].sort((a, b) => scoreOf(b) - scoreOf(a));
      } else {
        list = [...list].sort((a, b) => yearOf(b) - yearOf(a));
      }
      const ratings = {};
      const items = list.map((i) => {
        const mediaType = i.media_type === 'animation' ? 'animation'
          : i.media_type === 'anime' ? 'anime'
            : i.media_type === 'tv' ? 'tv' : 'movie';
        const tmdbId = i.id_tmdb;
        ratings[`${mediaType}:${tmdbId}`] = {
          kp: i.ratings?.kinopoisk?.score ?? null,
          imdb: i.ratings?.imdb?.score ?? null,
          kinopoiskFilmId: i.id_kp,
        };
        return {
          id: tmdbId,
          title: i.title.ru,
          originalTitle: i.title.original,
          posterUrl: i.poster,
          backdropUrl: i.backdrop || null,
          year: String(yearOf(i)),
          mediaType,
          kinopoiskFilmId: i.id_kp,
        };
      });
      return json(res, 200, { items, ratings, builtAt: Date.now() });
    }

    if (method === 'GET' && pathname === '/api/latest-trailers') {
      const start = Math.max(0, Number(q.get('start') || 0));
      const limit = Math.min(20, Math.max(1, Number(q.get('limit') || 6)));
      const all = catalog
        .filter((i) => i.trailer)
        .map((i) => {
          const yt = (i.trailer.url || '').match(/[?&]v=([^&]+)/);
          return {
            mediaType: i.media_type === 'tv' || i.media_type === 'anime' ? 'tv' : 'movie',
            tmdbId: i.id_tmdb,
            title: i.title.ru,
            releaseYear: yearOf(i),
            backdropUrl: i.backdrop || i.poster,
            youtubeKey: yt ? yt[1] : 'dQw4w9WgXcQ',
            trailerTitle: i.trailer.title || `${i.title.ru} — Трейлер`,
            publishedAt: (i.trailer.date ? `${i.trailer.date}T12:00:00.000Z` : '2026-01-01T00:00:00.000Z'),
          };
        });
      const slice = all.slice(start, start + limit);
      return json(res, 200, {
        items: slice,
        nextStart: start + slice.length,
        hasMore: start + slice.length < all.length,
        feedVersion: 1,
      });
    }

    if (method === 'GET' && pathname === '/api/player-config') {
      const kpId = Number(q.get('kinopoiskId'));
      const tmdbId = Number(q.get('tmdbId'));
      const found = findById(kpId) || findById(tmdbId);
      if (!found) {
        return json(res, 451, { error: 'taken_down' });
      }
      // Форма как initialPlayerConfig на сайте — полный набор провайдеров
      const yt =
        found.trailer?.url ||
        'https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=0&rel=0';
      const kp = String(found.id_kp || kpId || '');
      return json(res, 200, {
        isDemo: true,
        available: true,
        primaryPlayer: 'alloha',
        qualities: ['1080p', '720p', '480p'],
        dubs: ['Оригинал'],
        iframe_url: yt,
        collapsUrl: 'https://www.youtube.com/embed/LXb3EKWsInQ?autoplay=0&rel=0',
        resolvedKinopoiskId: kp,
        alloha: {
          baseUrl: yt,
          token: 'demo-token',
        },
        turbo: {
          baseUrl: 'https://www.youtube.com/embed/aqz-KE-bpKQ?autoplay=0&rel=0',
          siteId: 0,
          titleId: found.id_kp || 0,
          matchedBy: 'kinopoisk',
        },
        kodik: null,
        vibix: {
          publisherId: 'demo',
          embedId: '1',
          embedType: found.media_type === 'tv' || found.media_type === 'anime' ? 'serial' : 'movie',
        },
        videoseed: {
          baseUrl: 'https://www.youtube.com/embed/aqz-KE-bpKQ?autoplay=0&rel=0',
          type: found.media_type === 'tv' ? 'serial' : 'movie',
        },
        cvh: { publisherId: '10231' },
        veoveo: {
          baseUrl: 'https://www.youtube.com/embed/LXb3EKWsInQ?autoplay=0&rel=0',
        },
        capabilities: {
          collapsCatalog: true,
          allohaPlayerPass: true,
          allohaApi: true,
          kodik: false,
          turbo: true,
          vibix: true,
          videoseed: true,
          cvh: true,
          veoveo: true,
        },
      });
    }

    if (method === 'POST' && pathname === '/api/warm/cards') {
      const paths = Array.isArray(body?.paths) ? body.paths : [];
      const valid = paths.filter((p) => /^\/(film|series|movie)\/\d+$/.test(p));
      return json(res, 200, {
        warmed: valid.length,
        cached: 0,
        skipped: paths.length - valid.length,
        deferred: [],
      });
    }

    if (method === 'POST' && pathname === '/api/player-tags/posters') {
      const itemIds = Array.isArray(body?.itemIds) ? body.itemIds : [];
      const tags = {};
      for (const itemId of itemIds) {
        tags[itemId] = ['good_quality', 'dub'];
      }
      return json(res, 200, { tags });
    }

    if (method === 'POST' && pathname === '/api/tv-last-air/batch') {
      const tmdbIds = Array.isArray(body?.tmdbIds) ? body.tmdbIds : [];
      const items = {};
      for (const tmdbId of tmdbIds) {
        const found = catalog.find((c) => c.id_tmdb === Number(tmdbId));
        const seasons = found?.totals?.seasons || 1;
        const episodes = found?.totals?.episodes || 1;
        const ep = Math.min(5, episodes);
        const fp = `${seasons}|${ep}|2026-11-01`;
        items[String(tmdbId)] = {
          tmdbId: Number(tmdbId),
          fp,
          badge: found?.status === 'SERIAL_COMPLETE'
            ? 'СЕРИАЛ ЗАВЕРШЁН'
            : `${seasons} СЕЗОН ${ep} СЕРИЯ`,
          posterUrl: found?.poster || null,
          meta: {
            tmdbId: Number(tmdbId),
            seasonNumber: seasons,
            episodeNumber: ep,
            airDate: '2026-11-01',
            fingerprint: fp,
            showStatus: found?.status === 'SERIAL_COMPLETE' ? 'Ended' : 'Returning Series',
            inProduction: found?.status !== 'SERIAL_COMPLETE',
            nextSeasonNumber: found?.status === 'SERIAL_COMPLETE' ? null : seasons,
            nextEpisodeNumber: found?.status === 'SERIAL_COMPLETE' ? null : ep + 1,
            nextAirDate: found?.status === 'SERIAL_COMPLETE' ? null : '2026-11-08',
            airAt: Date.parse('2026-11-01T20:00:00Z'),
            airPrecision: 'day',
          },
        };
      }
      return json(res, 200, { items });
    }

    // ---- badges / ideas ----
    if (method === 'GET' && pathname === '/api/badges/public') {
      return json(res, 200, { ideas: store.ideas.length, dubs: 0 });
    }

    {
      const m = matchRoute(pathname, '/api/badges/{username}');
      if (method === 'GET' && m) {
        return json(res, 200, {
          ideas: store.ideas.length,
          dubs: 0,
          reviews: Object.keys(store.ratings).length,
          timings: Object.values(store.timings).flat().length,
          marathon_completed: store.marathonCompleted.length,
          ratings: Object.keys(store.ratings).length,
        });
      }
    }

    if (pathname === '/api/ideas') {
      if (method === 'GET') return json(res, 200, store.ideas);
      if (method === 'POST') {
        if (!requireAuth(req, res)) return;
        const idea = {
          id: store.ideas.length + 1,
          title: body?.title || 'Без названия',
          description: body?.description || '',
          votes: 0,
          status: 'open',
          author: {
            id: auth.user.id,
            username: auth.user.username,
            avatar: auth.user.avatar,
          },
          created_at: new Date().toISOString(),
        };
        store.ideas.unshift(idea);
        return json(res, 201, idea);
      }
    }

    return notFound(res);
  } catch (e) {
    return json(res, 500, { message: String(e.message || e), code: 'INTERNAL' });
  }
});

server.listen(PORT, HOST, () => {
  console.log(`YoHo mock API (full OpenAPI) → http://127.0.0.1:${PORT}`);
  console.log(`Android emulator → http://10.0.2.2:${PORT}`);
});
