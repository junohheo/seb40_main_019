import axios from 'axios';
axios.defaults.withCredentials = true;
axios.defaults.headers.common['Content-Type'] = 'application/json';

export const kakaoLogin = () => {
  window.location.href = `https://kauth.kakao.com/oauth/authorize?client_id=${process.env.REACT_APP_KAKAO_CLIENT_ID}&redirect_uri=${process.env.REACT_APP_KAKAO_REDIRECT_URI}&response_type=code&state=kakao`;
};

export const kakaoCallback = async (url) => {
  if (url.search) {
    const authorizationCode = url.search.split('=')[1].split('&')[0];
    console.log(authorizationCode);
    const res = await axios.post(
      `${process.env.REACT_APP_API_URL}oauth/kakao`,
      { authorizationCode }
    );
    console.log(res);
    // checkIsLogin(result);
    // window.location.replace('/');
    // if (res.status === 200) {
    //   let jwtToken = res.headers.get('authorization');
    //   let userData = res.data;
    //   window.sessionStorage.setItem('jwtToken', JSON.stringify(jwtToken));
    //   window.sessionStorage.setItem('userData', JSON.stringify(userData));
    //   // window.location.replace('/');
    // }
  }
};
