import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { QuillEditorComponent } from 'ngx-quill';
import { ContractTemplateService } from '../../../core/services/contract-template.service';
import { RentalType } from '../../../core/models/contract-template.model';

@Component({
  selector: 'app-template-form',
  templateUrl: './template-form.component.html',
  styleUrls: ['./template-form.component.css']
})
export class TemplateFormComponent implements OnInit {
  @ViewChild('editor') editor!: QuillEditorComponent;

  templateForm: FormGroup;
  loading = false;
  submitting = false;
  error: string | null = null;
  isEditMode = false;
  templateId: string | null = null;

  // Enum pour le template
  rentalTypes = Object.keys(RentalType).map(key => ({
    value: RentalType[key as keyof typeof RentalType],
    label: key === 'LONG_TERM' ? 'Location longue durée' : 'Location courte durée'
  }));

  // Dernière position du curseur dans l'éditeur Quill
  lastCursorPosition = 0;

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
    private fb: FormBuilder,
    private templateService: ContractTemplateService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.templateForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      description: ['', Validators.maxLength(500)],
      rentalType: [RentalType.LONG_TERM, Validators.required],
      content: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.templateId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.templateId;

    if (this.isEditMode && this.templateId) {
      this.loadTemplate(this.templateId);
    }
  }

  loadTemplate(id: string): void {
    this.loading = true;
    this.error = null;

    this.templateService.getTemplate(id).subscribe({
      next: (template) => {
        if (template.isDefault) {
          this.error = 'Les templates par défaut ne peuvent pas être modifiés';
          this.loading = false;
          return;
        }

        this.templateForm.patchValue({
          name: template.name,
          description: template.description,
          rentalType: template.rentalType,
          content: template.content
        });
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading template:', err);
        this.error = 'Erreur lors du chargement du template';
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

  onSubmit(): void {
    if (this.templateForm.invalid) {
      this.templateForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.error = null;

    const formValue = this.templateForm.value;

    if (this.isEditMode && this.templateId) {
      // Mode édition
      this.templateService.updateTemplate(this.templateId, formValue).subscribe({
        next: () => {
          this.submitting = false;
          this.router.navigate(['/templates']);
        },
        error: (err) => {
          console.error('Error updating template:', err);
          this.error = err.error?.message || 'Erreur lors de la mise à jour du template';
          this.submitting = false;
        }
      });
    } else {
      // Mode création
      this.templateService.createTemplate(formValue).subscribe({
        next: () => {
          this.submitting = false;
          this.router.navigate(['/templates']);
        },
        error: (err) => {
          console.error('Error creating template:', err);
          this.error = err.error?.message || 'Erreur lors de la création du template';
          this.submitting = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/templates']);
  }
}
