/**
 * app.js
 * Chứa logic chung cho toàn bộ ứng dụng (Quản lý Token, gọi API, Đăng xuất)
 */

const App = {
    // Lưu Token vào LocalStorage
    setToken(token) {
        if (token) {
            localStorage.setItem("token", token);
        }
    },

    // Lấy Token từ LocalStorage
    getToken() {
        return localStorage.getItem("token");
    },

    // Xóa Token và thông tin User
    clearAuth() {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
    },

    // Hàm gọi API chung, tự động đính kèm Token
    async callApi(url, options = {}) {
        const token = this.getToken();
        
        const headers = {
            "Content-Type": "application/json",
            ...options.headers
        };

        if (token) {
            headers["Authorization"] = "Bearer " + token;
        }

        const response = await fetch(url, {
            ...options,
            headers
        });

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
    logout() {
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

        if (token) {
            this.setToken(token);
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

// Khởi tạo các sự kiện chung (như nút đăng xuất nếu có)
document.addEventListener("DOMContentLoaded", () => {
    const logoutBtns = document.querySelectorAll('.logout-btn');
    logoutBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            App.logout();
        });
    });
});
