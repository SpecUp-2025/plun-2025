import axios from 'axios';
import router from '@/router';

const instance = axios.create({
    baseURL: `/api`
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
    async (error) =>{
        const status = error.response?.status;
        if(status===401){
            console.log("재발급 시도 중")
            const retry = error.config;
            if(retry && !retry.try){
                retry.try = true;
                try {
                    await newAccessToken();
                    console.log("재발급 완료")
                return instance(retry);
                } catch (error) {
                    localStorage.removeItem('accessToken')
                    localStorage.removeItem('refreshToken')
                    alert('세션이 만료되었습니다. 다시로그인해 주세요')
                    router.replace({name: 'login'});
                }
                
            }
        }
        return Promise.reject(error)
        }
)

async function newAccessToken(){
    const refreshToken = localStorage.getItem("refreshToken");
    if (!refreshToken || !refreshToken.trim()) {
        throw new Error('No refresh token');        
    }
    try {
        const {status,data} = await axios.post(`/api/auth/newAcessToken`,{
            refreshToken : refreshToken
        })
        if(status===200){
            localStorage.setItem('accessToken',data.accessToken)
            localStorage.setItem('refreshToken',data.refreshToken)
        }
        return data.accessToken;

    } catch (error) {
        console.error("accessToken 재발급 실패" ,error);
        throw error;
    }
    

}

export default instance;