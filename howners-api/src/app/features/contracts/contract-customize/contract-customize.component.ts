import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { QuillEditorComponent } from 'ngx-quill';
import { ContractService } from '../../../core/services/contract.service';
import { ContractTemplateService } from '../../../core/services/contract-template.service';
import { CreateContractRequest, UpdateContractRequest } from '../../../core/models/contract.model';

@Component({
  selector: 'app-contract-customize',
  templateUrl: './contract-customize.component.html',
  styleUrls: ['./contract-customize.component.css']
})
export class ContractCustomizeComponent implements OnInit {
  @ViewChild('editor') editor!: QuillEditorComponent;

  loading = false;
  submitting = false;
  error: string | null = null;
  rentalId: string | null = null;
  templateId: string | null = null;
  rentalPropertyName = '';
  tenantFullName = '';

  // Mode édition (contrat DRAFT existant)
  isEditMode = false;
  contractId: string | null = null;

  // Dernière position du curseur dans l'éditeur Quill
  lastCursorPosition = 0;

  // Contenu initial à injecter dans Quill une fois l'éditeur prêt
  private initialContent: string | null = null;
  private editorReady = false;

  // Configuration Quill
  quillModules = {
    toolbar: [
      ['bold', 'italic', 'underline'],
      [{ 'header': [1, 2, 3, false] }],
      [{ 'list': 'ordered'}, { 'list': 'bullet' }],
      [{ 'align': [] }],
      ['clean']
    ]
  };

  constructor(
    private contractService: ContractService,
    private templateService: ContractTemplateService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.contractId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.contractId;

    if (this.isEditMode && this.contractId) {
      this.loadExistingContract(this.contractId);
    } else {
      this.route.queryParams.subscribe(params => {
        this.rentalId = params['rentalId'];
        this.templateId = params['templateId'] || null;

        if (!this.rentalId) {
          this.error = 'ID de location manquant';
          return;
        }

        this.loadPreview();
      });
    }
  }

  /**
   * Appelé par Quill quand l'éditeur est prêt.
   * C'est le seul moment fiable pour injecter du contenu HTML.
   */
  onEditorCreated(quill: any): void {
    this.editorReady = true;
    if (this.initialContent) {
      quill.clipboard.dangerouslyPasteHTML(this.initialContent);
      this.initialContent = null;
    }
  }

  /**
   * Injecte le contenu dans Quill (si prêt) ou le met en attente.
   */
  private setEditorContent(html: string): void {
    if (this.editorReady && this.editor?.quillEditor) {
      this.editor.quillEditor.clipboard.dangerouslyPasteHTML(html);
    } else {
      this.initialContent = html;
    }
  }

  /**
   * Lit le contenu HTML directement depuis l'instance Quill.
   */
  private getEditorContent(): string {
    if (this.editor?.quillEditor) {
      return this.editor.quillEditor.root.innerHTML;
    }
    return '';
  }

  loadPreview(): void {
    if (!this.rentalId) return;

    this.loading = true;
    this.error = null;

    if (this.templateId) {
      this.templateService.previewTemplate(this.templateId, this.rentalId).subscribe({
        next: (preview) => {
          this.rentalPropertyName = preview.rentalPropertyName;
          this.tenantFullName = preview.tenantFullName;
          this.initialContent = preview.filledContent;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading preview:', err);
          this.error = 'Erreur lors du chargement de la prévisualisation';
          this.loading = false;
        }
      });
    } else {
      this.error = 'Veuillez sélectionner un template';
      this.loading = false;
    }
  }

  loadExistingContract(id: string): void {
    this.loading = true;
    this.error = null;

    this.contractService.getContract(id).subscribe({
      next: (contract) => {
        this.rentalPropertyName = contract.rentalPropertyName;
        this.tenantFullName = contract.tenantFullName;

        this.contractService.getContractVersions(id).subscribe({
          next: (versions) => {
            if (versions.length > 0) {
              const latestVersion = versions.sort((a, b) => b.version - a.version)[0];
              this.initialContent = latestVersion.content;
            }
            this.loading = false;
          },
          error: (err) => {
            console.error('Error loading contract versions:', err);
            this.error = 'Erreur lors du chargement des versions du contrat';
            this.loading = false;
          }
        });
      },
      error: (err) => {
        console.error('Error loading contract:', err);
        this.error = 'Erreur lors du chargement du contrat';
        this.loading = false;
      }
    });
  }

  onEditorSelectionChanged(event: any): void {
    if (event.range) {
      this.lastCursorPosition = event.range.index;
    }
  }

  onVariableSelected(variable: string): void {
    if (this.editor && this.editor.quillEditor) {
      const quill = this.editor.quillEditor;
      const range = quill.getSelection();
      const insertIndex = range ? range.index : this.lastCursorPosition;

      quill.focus();
      quill.insertText(insertIndex, variable);
      quill.setSelection(insertIndex + variable.length, 0);
    }
  }

  generateContract(): void {
    // Lire le contenu directement depuis Quill (pas depuis le form control)
    const content = this.getEditorContent();

    if (!content || content === '<p><br></p>') {
      this.error = 'Le contenu du contrat est vide';
      return;
    }

    if (!this.isEditMode && !this.rentalId) {
      return;
    }

    this.submitting = true;
    this.error = null;

    if (this.isEditMode && this.contractId) {
      const request: UpdateContractRequest = {
        customContent: content
      };

      this.contractService.updateContract(this.contractId, request).subscribe({
        next: (contract) => {
          this.submitting = false;
          this.router.navigate(['/contracts', contract.id]);
        },
        error: (err) => {
          console.error('Error updating contract:', err);
          this.error = err.error?.message || 'Erreur lors de la mise à jour du contrat';
          this.submitting = false;
        }
      });
    } else {
      const request: CreateContractRequest = {
        rentalId: this.rentalId!,
        templateId: this.templateId,
        customContent: content
      };

      this.contractService.createContract(request).subscribe({
        next: (contract) => {
          this.submitting = false;
          this.router.navigate(['/contracts', contract.id]);
        },
        error: (err) => {
          console.error('Error creating contract:', err);
          this.error = err.error?.message || 'Erreur lors de la création du contrat';
          this.submitting = false;
        }
      });
    }
  }

  cancel(): void {
    if (this.isEditMode && this.contractId) {
      this.router.navigate(['/contracts', this.contractId]);
    } else {
      this.router.navigate(['/contracts/new']);
    }
  }

  /**
   * Vérifie si l'éditeur a du contenu (pour activer/désactiver le bouton)
   */
  get hasContent(): boolean {
    if (this.editor?.quillEditor) {
      const text = this.editor.quillEditor.getText().trim();
      return text.length > 0;
    }
    return false;
  }
}
