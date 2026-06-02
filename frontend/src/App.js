import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import FormList from './pages/FormList';
import FormEditor from './pages/FormEditor';
import FormPreview from './pages/FormPreview';
import './App.css';

function App() {
  return (
    <Router>
      <div className="app">
        <Routes>
          <Route path="/" element={<FormList />} />
          <Route path="/editor/:id" element={<FormEditor />} />
          <Route path="/preview/:id" element={<FormPreview />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
