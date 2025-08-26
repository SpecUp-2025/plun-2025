import axios from 'axios';

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
            error.response.statusText = "Unauthorized";
            error.response.status=401;
            
            const retry = error.config;
            if(retry && !retry.try){
                retry.try = true;
                await newAcessToken();
                return instance(retry);
            }
        }
    return Promise.reject(error)
    }
)

async function newAcessToken(){
    const refreshToken = localStorage.getItem("refreshToken");
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
    }
    

}

export default instance;