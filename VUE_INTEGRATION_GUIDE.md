# Guide: How to connect Vue.js (Axios) to Jenka Coffee API

This guide explains how to connect your new Vue.js frontend to the Spring Boot REST API we just created.

## 1. Configure Axios in Vue.js

Since the backend still relies on `HttpSession` to maintain login state, **you must configure Axios to send the session cookie (`JSESSIONID`) with every request.**

In your Vue.js project, configure your Axios instance like this:

```javascript
// src/api/axios.js
import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api', // Point to your Spring Boot API
    withCredentials: true, // CRITICAL: This allows Axios to send/receive cookies
    headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    }
});

export default api;
```

## 2. Implement Login Flow

When a user logs in, the API sets a `JSESSIONID` cookie in the browser.

```javascript
// Example Vue Login Action
import api from '@/api/axios';

async function login(username, password, remember) {
    try {
        const response = await api.post('/auth/login', {
            username: username,
            password: password,
            remember: remember
        });
        
        console.log(response.data.message); // "Đăng nhập thành công"
        // Save the user data to Vuex or Pinia
        const user = response.data.data; 
        return user;
    } catch (error) {
        console.error("Login failed:", error.response.data.message);
    }
}
```

## 3. Checking Authentication State on Reload

When a user refreshes the page, you can check if they are still logged in by calling the `/api/auth/me` endpoint. Because Axios is configured with `withCredentials: true`, it will automatically send the session cookie to the server.

```javascript
// Example Vue Bootstrapping (in App.vue or Router guard)
import api from '@/api/axios';

async function checkAuthStatus() {
    try {
        const response = await api.get('/auth/me');
        const user = response.data.data;
        // The user is logged in, continue...
    } catch (error) {
        // The user is not logged in or the session expired.
        // Redirect to login page.
    }
}
```

## 4. Fetching Data (Products/Categories)

The existing Thymeleaf logic is unaffected. Your Vue components can now fetch data cleanly.

```javascript
import api from '@/api/axios';

async function fetchProducts(page = 0, size = 12) {
    const response = await api.get(`/products?page=${page}&size=${size}`);
    const products = response.data.data.items;
    const totalPages = response.data.data.totalPages;
    // Update Vue state
}
```

## Moving Forward

You now have a parallel architecture:
1. `http://localhost:8080/` -> Still serves your old Thymeleaf pages.
2. `http://localhost:8080/api/...` -> Serves raw JSON data for Vue.js.

You can continue building more API Controllers (e.g., `ApiCartController`, `ApiOrderController`) using the same pattern!
