import '../css/SellerAddProduct.scss';
import { Link } from 'react-router-dom';
import ProductForm from '../../../components/seller/js/ProductForm';
import { useState } from 'react';
import ModalOk from '../../../components/modal/js/ModalOk';
import { handleSubmit } from '../../../util/api/product';
// import { useSelector } from 'react-redux';

export default function SellerAddProduct() {
  // const loginData = useSelector((state) => state.login);
  const [modalOn, setModalOn] = useState(false);

  const [categoryId, setCategoryId] = useState('1');
  const [productName, setProductName] = useState('');
  const [price, setPrice] = useState(0);
  const [titleImg, setTitleImg] = useState([]);
  const [detailImg, setDetailImg] = useState([]);

  const data = {
    categoryId,
    productName,
    price,
    titleImg,
    detailImg,
    tag: 'new', //비엇으면 빈문자열로 보냄
  };

  const formSubmit = (e) => {
    e.preventDefault();
    handleSubmit(data, setModalOn);
  };

  return (
    <div className="sellerAddProduct">
      <div className="productTitle">
        <h1>상품 등록</h1>
      </div>
      <ProductForm
        categoryId={categoryId}
        setCategoryId={setCategoryId}
        productName={productName}
        setProductName={setProductName}
        price={price}
        setPrice={setPrice}
        titleImg={titleImg}
        setTitleImg={setTitleImg}
        detailImg={detailImg}
        setDetailImg={setDetailImg}
      />
      <div className="addPageBtns">
        <Link to="/seller/product">
          <button className="close">닫기</button>
        </Link>
        <button onClick={formSubmit}>저장하기</button>
      </div>
      <ModalOk setClick={setModalOn} modalOn={modalOn} />
    </div>
  );
}
