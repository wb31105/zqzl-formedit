import { fieldTypes } from '../utils/fieldTypes';

function ControlLibrary({ onDragStart }) {
  const handleDragStart = (e, type) => {
    e.dataTransfer.setData('fieldType', type);
    e.dataTransfer.effectAllowed = 'copy';
    onDragStart && onDragStart(type);
  };

  return (
    <div className="editor-sidebar">
      <h2>控件库</h2>
      <div className="control-library">
        {fieldTypes.map((item) => (
          <div
            key={item.type}
            className="control-item"
            draggable
            onDragStart={(e) => handleDragStart(e, item.type)}
          >
            <div className="control-icon">{item.icon}</div>
            <span className="control-name">{item.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default ControlLibrary;
