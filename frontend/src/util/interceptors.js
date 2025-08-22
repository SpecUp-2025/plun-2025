import axios from 'axios';

const instance = axios.create({
    baseURL: '/api'
})

instance.interceptors.request.use(
    (config) => {
        const accessToken = localStorage.getItem('accessToken');
        if(accessToken){
            config.headers = config.headers || {};
            config.headers.Authorization = `Bearer ${accessToken}`;
        }
        return config;
    },
    (error) =>{
        return Promise.reject(error);
    },
);

instance.interceptors.response.use(
    (response) =>{
        return response;
    },
    (error) =>{
        const status = error.response?.status;
        if(status===401){
            error.response.statusText = "Unauthorized";
            error.response.status=401;
            // access토큰 재발급 넣기
        }
    return Promise.reject(error)
    }
)

export default instance;