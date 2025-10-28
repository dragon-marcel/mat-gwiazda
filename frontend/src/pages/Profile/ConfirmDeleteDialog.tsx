import React from 'react';
import { Button } from '../../components/ui/Button';

type Props = {
  open: boolean;
  title?: string;
  message?: string;
  onConfirm: () => void;
  onCancel: () => void;
};

const ConfirmDeleteDialog: React.FC<Props> = ({ open, title = 'Potwierdź', message = 'Czy na pewno chcesz usunąć swoje konto? Ta operacja jest nieodwracalna.', onConfirm, onCancel }) => {
  if (!open) return null;

  return (
    <div role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title" className="fixed inset-0 z-50 flex items-center justify-center">
      {/* backdrop */}
      <div className="absolute inset-0 bg-black/40" onClick={onCancel} />

      {/* modal panel */}
      <div className="relative bg-slate-900 rounded-lg p-6 w-[90%] max-w-md mx-4 text-center shadow-lg text-white">
        <h3 id="confirm-dialog-title" className="text-lg font-semibold mb-2">{title}</h3>
        <p className="mb-4 text-sm text-white">{message}</p>
        <div className="flex justify-center gap-3">
          <Button variant="outline" size="sm" onClick={onCancel} className="bg-gray-700 text-white hover:bg-gray-600">Anuluj</Button>
          <Button variant="destructive" size="sm" onClick={onConfirm} className="bg-red-600 hover:bg-red-700 text-white">Usuń konto</Button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmDeleteDialog;
