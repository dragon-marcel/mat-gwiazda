import React, { useState } from 'react';
import ConfirmDeleteDialog from './ConfirmDeleteDialog';
import { Button } from '../../components/ui/Button';

type Props = {
  onDelete: () => void;
};

const DangerZone: React.FC<Props> = ({ onDelete }) => {
  const [open, setOpen] = useState(false);

  return (
    <div className="bg-slate-800 p-4 rounded-md shadow-sm text-white">
      <h3 className="text-md font-medium mb-2">Strefa niebezpieczeństwa</h3>
      <p className="text-sm text-white mb-4">Usunięcie konta spowoduje utratę wszystkich danych. Operacja jest nieodwracalna.</p>
      <Button
        variant="destructive"
        size="default"
        className="w-full px-4 py-3 shadow-md bg-red-600 hover:bg-red-700 text-white"
        onClick={() => setOpen(true)}
      >
        Usuń konto
      </Button>

      <ConfirmDeleteDialog
        open={open}
        onCancel={() => setOpen(false)}
        onConfirm={() => {
          setOpen(false);
          onDelete();
        }}
      />
    </div>
  );
};

export default DangerZone;
