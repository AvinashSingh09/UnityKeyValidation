import { useState, useEffect } from 'react';
import { getProducts, createProduct, updateProduct, deleteProduct } from '../../api/productApi';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';
import { HiOutlinePlus, HiOutlinePencil, HiOutlineTrash, HiOutlineCube } from 'react-icons/hi2';
import './Products.css';

export default function ProductsPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editProduct, setEditProduct] = useState(null);
  const [form, setForm] = useState({ name: '', productCode: '', description: '', version: '' });
  const { isAdmin, isSuperAdmin } = useAuth();

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      const { data } = await getProducts();
      setProducts(data);
    } catch (err) {
      toast.error('Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  const openCreate = () => {
    setEditProduct(null);
    setForm({ name: '', productCode: '', description: '', version: '' });
    setShowModal(true);
  };

  const openEdit = (product) => {
    setEditProduct(product);
    setForm({
      name: product.name,
      productCode: product.productCode,
      description: product.description || '',
      version: product.version || '',
    });
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editProduct) {
        await updateProduct(editProduct.id, form);
        toast.success('Product updated');
      } else {
        await createProduct(form);
        toast.success('Product created');
      }
      setShowModal(false);
      fetchProducts();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to deactivate this product?')) return;
    try {
      await deleteProduct(id);
      toast.success('Product deactivated');
      fetchProducts();
    } catch (err) {
      toast.error('Failed to delete product');
    }
  };

  if (loading) return <div className="loader"><div className="spinner" /></div>;

  return (
    <div className="products-page">
      <div className="page-header">
        <div>
          <h1>Products</h1>
          <p>Manage your Unity applications</p>
        </div>
        {isAdmin() && (
          <button onClick={openCreate} className="btn btn-primary">
            <HiOutlinePlus /> Add Product
          </button>
        )}
      </div>

      {products.length === 0 ? (
        <div className="card empty-state">
          <HiOutlineCube />
          <h3>No products yet</h3>
          <p>Create your first product to start generating license keys</p>
        </div>
      ) : (
        <div className="products-grid">
          {products.map((product) => (
            <div key={product.id} className="card product-card">
              <div className="product-card-header">
                <div className="product-icon">
                  <HiOutlineCube />
                </div>
                <div className="product-actions">
                  {isAdmin() && (
                    <button onClick={() => openEdit(product)} className="btn btn-ghost btn-icon btn-sm">
                      <HiOutlinePencil />
                    </button>
                  )}
                  {isSuperAdmin() && (
                    <button onClick={() => handleDelete(product.id)} className="btn btn-ghost btn-icon btn-sm">
                      <HiOutlineTrash />
                    </button>
                  )}
                </div>
              </div>
              <h3 className="product-name">{product.name}</h3>
              <span className="product-code mono">{product.productCode}</span>
              {product.description && (
                <p className="product-desc">{product.description}</p>
              )}
              <div className="product-meta">
                {product.version && <span>v{product.version}</span>}
                <span className={`badge ${product.active ? 'badge-active' : 'badge-revoked'}`}>
                  {product.active ? 'Active' : 'Inactive'}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editProduct ? 'Edit Product' : 'New Product'}</h2>
              <button onClick={() => setShowModal(false)} className="btn btn-ghost btn-icon">✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="input-group">
                <label>Product Name</label>
                <input className="input" value={form.name} onChange={(e) => setForm({...form, name: e.target.value})} required placeholder="e.g. SteelVR" />
              </div>
              <div className="input-group mt-md">
                <label>Product Code</label>
                <input className="input mono" value={form.productCode} onChange={(e) => setForm({...form, productCode: e.target.value.toUpperCase()})} required placeholder="e.g. STEELVR-001" />
              </div>
              <div className="input-group mt-md">
                <label>Description</label>
                <input className="input" value={form.description} onChange={(e) => setForm({...form, description: e.target.value})} placeholder="Brief description" />
              </div>
              <div className="input-group mt-md">
                <label>Version</label>
                <input className="input" value={form.version} onChange={(e) => setForm({...form, version: e.target.value})} placeholder="e.g. 1.0.0" />
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setShowModal(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" className="btn btn-primary">{editProduct ? 'Update' : 'Create'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
