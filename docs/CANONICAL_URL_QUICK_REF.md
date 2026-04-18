# Canonical URL - Quick Reference Card

## 🚀 Quick Start (Copy & Paste)

### Basic SEO Setup (Most Common)

```vue
<script setup>
import { useRoute } from 'vue-router'
import { useSeoMeta } from '@/composables/useSeoMeta'

const route = useRoute()

useSeoMeta({
  title: 'Your Page Title',
  description: 'Your page description',
  route: route  // ← This auto-generates canonical URL
})
</script>
```

### With Auto-Redirect to Canonical URL

```vue
<script setup>
import { useRoute } from 'vue-router'
import { useSeoMeta } from '@/composables/useSeoMeta'
import { useCanonicalRedirect } from '@/composables/useCanonicalRedirect'

const route = useRoute()

// Auto-redirect if URL is not canonical
useCanonicalRedirect()

useSeoMeta({
  title: 'Your Page Title',
  description: 'Your page description',
  route: route
})
</script>
```

## 📋 Common Use Cases

### 1. Product Listing Page

```vue
<script setup>
import { useRoute } from 'vue-router'
import { useSeoMeta } from '@/composables/useSeoMeta'
import { useCanonicalRedirect } from '@/composables/useCanonicalRedirect'

const route = useRoute()

// Redirect messy URLs to clean canonical URL
useCanonicalRedirect()

useSeoMeta({
  title: 'Sản Phẩm',
  description: 'Danh sách sản phẩm',
  route: route
})
</script>
```

**Before:** `https://jenkacoffee.com/San-Pham?page=2&sort=price`  
**After:** `https://jenkacoffee.com/san-pham`

### 2. Category Page with Filters

```vue
<script setup>
import { useRoute } from 'vue-router'
import { useSeoMeta } from '@/composables/useSeoMeta'

const route = useRoute()

useSeoMeta({
  title: 'Máy Pha Cà Phê',
  description: 'Danh mục máy pha cà phê',
  route: route
})
</script>
```

**Before:** `https://jenkacoffee.com/category/coffee?page=1&sort=newest`  
**After:** `https://jenkacoffee.com/category/coffee`

### 3. Generate Slug from Vietnamese Text

```javascript
import { normalizeVietnameseSlug } from '@/utils/canonicalUrl'

const productName = 'Máy Pha Cà Phê DeLonghi'
const slug = normalizeVietnameseSlug(productName)
// Result: 'may-pha-ca-phe-delonghi'
```

## 🎯 What Gets Normalized?

### Path Normalization

| Input | Output |
|-------|--------|
| `/San-Pham` | `/san-pham` |
| `/san-pham//may-pha` | `/san-pham/may-pha` |
| `/san-pham/` | `/san-pham` |
| `/may--pha--ca-phe` | `/may-pha-ca-phe` |

### Query Params

| Param | Action | Reason |
|-------|--------|--------|
| `category` | ✅ Keep | SEO-relevant filter |
| `brand` | ✅ Keep | SEO-relevant filter |
| `page` | ❌ Remove | UI-only (pagination) |
| `sort` | ❌ Remove | UI-only (sort order) |
| `view` | ❌ Remove | UI-only (grid/list) |
| `limit` | ❌ Remove | UI-only (items per page) |

## 🔧 Configuration

### Change Domain

Edit `src/utils/canonicalUrl.js`:

```javascript
const CANONICAL_DOMAIN = 'https://jenkacoffee.com' // ← Change here
```

### Add Core Param (Keep in URL)

Edit `src/utils/canonicalUrl.js`:

```javascript
const CORE_PARAMS = ['category', 'brand', 'color'] // ← Add 'color'
```

### Add UI Param (Remove from URL)

Edit `src/utils/canonicalUrl.js`:

```javascript
const UI_PARAMS = ['page', 'sort', 'view', 'tab'] // ← Add 'tab'
```

## 🧪 Testing

### Manual Test in Browser Console

```javascript
import { generateCanonicalUrl } from '@/utils/canonicalUrl'

const route = {
  path: '/San-Pham//May--Pha/',
  query: { category: 'coffee', page: '2' }
}

console.log(generateCanonicalUrl(route))
// Output: https://jenkacoffee.com/san-pham/may-pha?category=coffee
```

### Run Test Suite

```javascript
// Import test file in your component
import '@/utils/canonicalUrl.test.js'

// Check console for test results
```

## ⚠️ Common Mistakes

### ❌ Wrong: Manual URL Construction

```javascript
const url = `https://jenkacoffee.com${route.path}?${route.query}`
```

### ✅ Correct: Use Generator

```javascript
import { generateCanonicalUrl } from '@/utils/canonicalUrl'
const url = generateCanonicalUrl(route)
```

### ❌ Wrong: Forgot to Pass Route

```javascript
useSeoMeta({
  title: 'Products'
  // Missing: route parameter
})
```

### ✅ Correct: Pass Route Object

```javascript
useSeoMeta({
  title: 'Products',
  route: route  // ← Don't forget this!
})
```

## 📊 SEO Impact

### Before Canonical URLs

```
❌ https://jenkacoffee.com/san-pham?page=1
❌ https://jenkacoffee.com/san-pham?page=2
❌ https://jenkacoffee.com/San-Pham
❌ https://jenkacoffee.com/san-pham/
```

**Problem:** Google sees 4 duplicate pages, splits link equity

### After Canonical URLs

```
✅ https://jenkacoffee.com/san-pham (canonical)
```

**Result:** All link equity consolidated to 1 URL

## 🎓 Key Concepts

### Pure Function

Same input → Same output (always)

```javascript
generateCanonicalUrl(route) === generateCanonicalUrl(route) // true
```

### No Side Effects

Doesn't modify input or external state

```javascript
const route = { path: '/test', query: {} }
generateCanonicalUrl(route)
// route is unchanged
```

### Deterministic

Predictable, testable, cacheable

```javascript
// Call 1000 times, get same result
for (let i = 0; i < 1000; i++) {
  console.log(generateCanonicalUrl(route)) // Always same
}
```

## 📚 Full Documentation

See `CANONICAL_URL_GUIDE.md` for complete documentation.

## 🆘 Need Help?

1. Check `CANONICAL_URL_GUIDE.md`
2. Run test suite: `import '@/utils/canonicalUrl.test.js'`
3. Contact development team

---

**Last Updated:** 2024  
**Version:** 1.0.0
