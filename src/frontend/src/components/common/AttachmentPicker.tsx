import type { ChangeEvent } from 'react';
import type { AttachmentReference } from '../../types/domain';

interface AttachmentPickerProps {
  files: File[];
  onFilesChange: (files: File[]) => void;
  existingAttachments?: AttachmentReference[];
}

export default function AttachmentPicker({ files, onFilesChange, existingAttachments }: AttachmentPickerProps) {
  return (
    <section className="detail-card">
      <h3>Attachments</h3>
      <input
        type="file"
        multiple
        onChange={(event: ChangeEvent<HTMLInputElement>) => onFilesChange(Array.from(event.target.files || []))}
      />
      <div className="version-list">
        {files.map(file => (
          <div key={`${file.name}-${file.size}`} className="version-row">
            <strong>{file.name}</strong>
            <span>{file.type || 'application/octet-stream'}</span>
          </div>
        ))}
        {files.length === 0 && <p className="muted-text">No new attachments selected.</p>}
      </div>
      {!!existingAttachments?.length && (
        <>
          <h4>Existing attachments</h4>
          <div className="attachment-table">
            <div className="attachment-row attachment-header-row">
              <span>Name</span>
              <span>Mimetype</span>
              <span>Size</span>
            </div>
            {existingAttachments.map(attachment => (
              <div key={attachment.id} className="attachment-row">
                <a href={attachment.downloadPath} target="_blank" rel="noreferrer">
                  {attachment.name}
                </a>
                <span>{attachment.mimeType}</span>
                <span>{attachment.sizeLabel}</span>
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}
