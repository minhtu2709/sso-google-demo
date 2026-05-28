/**
 * app.js
 * Chứa logic chung cho toàn bộ ứng dụng (Quản lý Token, gọi API, Đăng xuất)
 */

const App = {
    // Lưu Token vào LocalStorage
    setToken(token, refreshToken) {
        if (token) {
            localStorage.setItem("token", token);
        }
        if (refreshToken) {
            localStorage.setItem("refreshToken", refreshToken);
        }
    },

    // Lấy Token từ LocalStorage
    getToken() {
        return localStorage.getItem("token");
    },

    getRefreshToken() {
        return localStorage.getItem("refreshToken");
    },

    // Xóa Token và thông tin User
    clearAuth() {
        localStorage.removeItem("token");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");
    },

    // Hàm gọi API chung, tự động đính kèm Token
    async callApi(url, options = {}) {
        let token = this.getToken();

        const headers = {
            ...options.headers
        };

        // Nếu body không phải FormData, mặc định là JSON
        if (!(options.body instanceof FormData)) {
            headers["Content-Type"] = "application/json";
        }

        if (token) {
            headers["Authorization"] = "Bearer " + token;
        }

        let response = await fetch(url, {
            ...options,
            headers
        });

        // Xử lý khi Token hết hạn (401 Unauthorized)
        if (response.status === 401) {
            const refreshToken = this.getRefreshToken();
            if (refreshToken) {
                try {
                    // Thử refresh token
                    const refreshRes = await fetch("/auth/refresh", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ refreshToken })
                    });
                    const refreshResult = await refreshRes.json();

                    if (refreshRes.ok && refreshResult.success) {
                        const newData = refreshResult.data;
                        this.setToken(newData.token, newData.refreshToken);

                        // Thử lại request cũ với token mới
                        headers["Authorization"] = "Bearer " + newData.token;
                        response = await fetch(url, { ...options, headers });
                    } else {
                        throw new Error("Refresh failed");
                    }
                } catch (e) {
                    this.clearAuth();
                    alert("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại!");
                    window.location.href = "/login.html";
                    return;
                }
            } else {
                this.clearAuth();
                window.location.href = "/login.html";
                return;
            }
        }

        const result = await response.json();

        if (!response.ok || !result.success) {
            // Xử lý lỗi validation (nếu backend trả về object trong data)
            if (result.data && typeof result.data === "object" && !Array.isArray(result.data)) {
                const errors = Object.values(result.data).join("\n");
                throw new Error(errors || result.message || "Đã có lỗi xảy ra");
            }
            throw new Error(result.message || "Gọi API thất bại");
        }

        return result.data;
    },

    // Kiểm tra token đã hết hạn chưa (Đọc từ Payload của JWT)
    isTokenExpired() {
        const token = this.getToken();
        if (!token) return true;

        try {
            // JWT gồm 3 phần: header.payload.signature
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));

            const payload = JSON.parse(jsonPayload);
            const expiredAt = payload.exp * 1000; // Đổi giây sang ms
            return Date.now() > expiredAt;
        } catch (e) {
            return true; // Lỗi parse coi như hết hạn
        }
    },

    // Hàm kiểm tra nhanh quyền truy cập (Dùng khi load trang)
    checkAuth() {
        if (!this.getToken()) {
            this.clearAuth();
            window.location.href = "/login.html";
            return false;
        }
        // Nếu Access Token hết hạn nhưng có Refresh Token thì vẫn cho qua,
        // callApi sẽ tự refresh sau.
        if (this.isTokenExpired() && !this.getRefreshToken()) {
            this.clearAuth();
            window.location.href = "/login.html";
            return false;
        }
        return true;
    },

    // Hiển thị thông báo (Thành công / Thất bại) trên form
    showStatus(elementId, message, isSuccess = false) {
        const el = document.getElementById(elementId);
        if (!el) return;
        
        el.textContent = message;
        el.className = 'status-message ' + (isSuccess ? 'success' : 'error');
    },

    // Ẩn thông báo
    hideStatus(elementId) {
        const el = document.getElementById(elementId);
        if (!el) return;
        
        el.textContent = '';
        el.className = 'status-message';
    },

    // Xử lý đăng xuất chung
    logout(askConfirm = false) {
        if (askConfirm) {
            if (!confirm("Bạn có chắc chắn muốn đăng xuất không?")) {
                return;
            }
        }
        this.clearAuth();
        window.location.href = "/login.html";
    },

    // Kiểm tra và lưu token nếu có trên URL hash (Dùng cho OAuth2)
    saveTokenFromHash() {
        const hash = window.location.hash.startsWith("#")
            ? window.location.hash.substring(1)
            : "";

        const params = new URLSearchParams(hash);
        const token = params.get("token");
        const refreshToken = params.get("refreshToken");

        if (token) {
            this.setToken(token, refreshToken);
            // Xóa hash trên URL cho sạch
            history.replaceState(null, "", window.location.pathname);
            return true; // Trả về true nếu vừa lưu token xong
        }
        return false;
    },
    // Xử lý địa chỉ VN
    LocationAPI: {
        async getProvinces() {
            const res = await fetch("https://provinces.open-api.vn/api/p/");
            return res.json();
        },
        async getDistricts(provinceCode) {
            const res = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`);
            const data = await res.json();
            return data.districts || [];
        },
        async getWards(districtCode) {
            const res = await fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
            const data = await res.json();
            return data.wards || [];
        }
    }
};

// Kiểm tra token hết hạn định kỳ mỗi 1 phút
setInterval(() => {
    const token = App.getToken();
    const refreshToken = App.getRefreshToken();

    // Chỉ logout nếu cả 2 đều không hợp lệ/hết hạn
    if (token && App.isTokenExpired() && !refreshToken) {
        App.clearAuth();
        alert("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại!");
        window.location.href = "/login.html";
    }
}, 60 * 1000);

// Khởi tạo các sự kiện chung (như nút đăng xuất nếu có)
document.addEventListener("DOMContentLoaded", () => {
    const logoutBtns = document.querySelectorAll('.logout-btn');
    logoutBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            App.logout(true);
        });
    });
});
