import { useEffect } from 'react';
import './App.scss';
import { Route, Routes, useLocation } from 'react-router-dom';
import Layout from './components/layout/js/Layout';
import Home from './pages/home/js/Home';
import Login from './pages/login/js/Login';
import Signup from './pages/signup/js/Signup';
import OauthKakao from './pages/oauth/js/OauthKakao';
import OauthGoogle from './pages/oauth/js/OauthGoogle';
import { useDispatch } from 'react-redux';
import { login } from './redux/reducers/loginSlice';
import { setUser } from './redux/reducers/userSlice';
import { getCookie } from './util/cookie/cookie';
import ShopProductList from './pages/shopProductList/js/ShopProductList';
import ShopProductDetail from './pages/shopProductDetail/js/ShopProductDetail';
import ShopProductOrder from './pages/shopProductOrder/js/ShopProductOrder';
import { tokenReissue } from './util/api/Reissue';
import Seller from './pages/seller/js/Seller';
import SellerLayout from './components/layout/js/SellerLayout';
import ShopLayout from './components/layout/js/ShopLayout';
import Success from './pages/payment/js/Success';
import Failed from './pages/payment/js/Failed';
import SellerProducts from './pages/sellerProduct/js/SellerProducts';
import SellerAddProduct from './pages/sellerAddProduct/js/SellerAddProduct';
import SellerEditProduct from './pages/sellerEditProduct/js/SellerEditProduct';
import SellerOrder from './pages/sellerOrder/js/SellerOrder';
import SellerReview from './pages/sellerReview/js/SellerReview';
import MypageUserPage from './pages/mypage/user/js/MypageUserPage';
import MypageUserEditPage from './pages/mypage/userEdit/js/MypageUserEditPage';
import MypagePointPage from './pages/mypage/point/js/MypagePointPage';
import ReviewAdd from './pages/reviewAdd/js/ReviewAdd';
import ReviewEdit from './pages/reviewEdit/js/ReviewEdit';
import ShopMypageOrderList from './pages/shopMypageOrderList/js/ShopMypageOrderList';
import MypageLayout from './components/layout/js/MypageLayout';
import Review from './pages/review/js/Review';
import MypageHome from './pages/mypageHome/js/MypageHome';

function App() {
  const dispatch = useDispatch();

  //페이지이동시 맨위로
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  useEffect(() => {
    const userData = JSON.parse(window.sessionStorage.getItem('userData'));
    const accesstoken = JSON.parse(
      window.sessionStorage.getItem('accesstoken')
    );
    if (userData && accesstoken) {
      dispatch(setUser(userData));
      dispatch(login({ accesstoken }));
    } else {
      if (getCookie('refreshtoken')) {
        let res = tokenReissue(getCookie('refreshtoken'));
        res.then((data) => {
          if (data.status === 200) {
            dispatch(
              setUser(JSON.parse(window.sessionStorage.getItem('userData')))
            );
            dispatch(
              login(JSON.parse(window.sessionStorage.getItem('accesstoken')))
            );
          }
        });
      }
    }
  }, []);
  return (
    <>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/oauth/kakao" element={<OauthKakao />} />
          <Route path="/oauth/google" element={<OauthGoogle />} />
          <Route path="/payment/success" element={<Success />} />
          <Route path="/payment/failed" element={<Failed />} />

          <Route path="/" element={<ShopLayout />}>
            <Route path="/products" element={<ShopProductList />} />
            <Route path="/product/detail/:id" element={<ShopProductDetail />} />
            <Route path="/product/order" element={<ShopProductOrder />} />
          </Route>

          <Route path="/" element={<SellerLayout />}>
            <Route path="/seller" element={<Seller />} />
            <Route path="/seller/product" element={<SellerProducts />} />
            <Route path="/seller/add" element={<SellerAddProduct />} />
            <Route path="/seller/edit/:id" element={<SellerEditProduct />} />
            <Route path="/seller/order" element={<SellerOrder />} />
            <Route path="/seller/review" element={<SellerReview />} />
          </Route>

          <Route path="/" element={<MypageLayout />}>
            <Route path="/mypage" element={<MypageHome />} />
            <Route path="/mypage/user" element={<MypageUserPage />} />
            <Route path="/mypage/user/edit" element={<MypageUserEditPage />} />
            <Route path="/mypage/point" element={<MypagePointPage />} />
            <Route path="/mypage/review" element={<Review />} />
            <Route path="/mypage/reviewadd/:id" element={<ReviewAdd />} />
            <Route path="/mypage/reviewedit/:id" element={<ReviewEdit />} />
            <Route path="/mypage/order" element={<ShopMypageOrderList />} />
          </Route>
        </Route>
      </Routes>
    </>
  );
}

export default App;
