import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import FormList from './pages/FormList';
import FormEditor from './pages/FormEditor';
import FormPreview from './pages/FormPreview';
import WorkflowList from './pages/WorkflowList';
import WorkflowEditor from './pages/WorkflowEditor';
import WorkflowPreview from './pages/WorkflowPreview';
import WorkflowInstance from './pages/WorkflowInstance';
import WorkflowHelp from './pages/WorkflowHelp';
import './App.css';

function App() {
  return (
    <Router>
      <div className="app">
        <nav className="app-nav">
          <div className="nav-brand">数据标注平台</div>
          <div className="nav-links">
            <Link to="/" className="nav-link">表单管理</Link>
            <Link to="/workflows" className="nav-link">工作流管理</Link>
            <Link to="/workflow/help" className="nav-link">使用帮助</Link>
          </div>
        </nav>
        <div className="app-content">
          <Routes>
            <Route path="/" element={<FormList />} />
            <Route path="/editor/:id" element={<FormEditor />} />
            <Route path="/preview/:id" element={<FormPreview />} />
            <Route path="/workflows" element={<WorkflowList />} />
            <Route path="/workflow/editor/:id" element={<WorkflowEditor />} />
            <Route path="/workflow/preview/:id" element={<WorkflowPreview />} />
            <Route path="/workflow/instance/:id" element={<WorkflowInstance />} />
            <Route path="/workflow/instance/new/:definitionId" element={<WorkflowInstance />} />
            <Route path="/workflow/help" element={<WorkflowHelp />} />
          </Routes>
        </div>
      </div>
    </Router>
  );
}

export default App;
