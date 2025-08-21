import { useUserStore } from "@/store/userStore";
import instance from "./interceptors";

export async function getUser() {
    const userStore =useUserStore();
try {
    const {data} = await instance.get('/member/me');
    userStore.setUser(data)
} catch (error) {
    console.error("전역상태 저장 안됨",error)
}    
}