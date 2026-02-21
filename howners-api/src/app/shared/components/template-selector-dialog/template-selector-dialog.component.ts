import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { ContractTemplateService } from '../../../core/services/contract-template.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ContractTemplate, RentalType, PreviewTemplateResponse } from '../../../core/models/contract-template.model';

@Component({
  selector: 'app-template-selector-dialog',
  templateUrl: './template-selector-dialog.component.html',
  styleUrls: ['./template-selector-dialog.component.scss']
})
export class TemplateSelectorDialogComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Input properties (set by parent)
  @Input() rentalId: string = '';
  @Input() rentalType: RentalType | null = null;

  // Output events
  @Output() onConfirm = new EventEmitter<string | null>();
  @Output() onCancel = new EventEmitter<void>();

  // Data
  templates: ContractTemplate[] = [];
  selectedTemplate: ContractTemplate | null = null;
  preview: PreviewTemplateResponse | null = null;

  // Loading states
  loadingTemplates = false;
  loadingPreview = false;

  constructor(
    private templateService: ContractTemplateService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadTemplates(): void {
    this.loadingTemplates = true;
    this.templateService.getMyTemplates(this.rentalType || undefined)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingTemplates = false)
      )
      .subscribe({
        next: (templates) => {
          this.templates = templates;
          // Sélectionner le template par défaut automatiquement
          const defaultTemplate = templates.find(t => t.isDefault && t.rentalType === this.rentalType);
          if (defaultTemplate) {
            this.selectTemplate(defaultTemplate);
          }
        },
        error: (err) => {
          console.error('Error loading templates:', err);
          this.notificationService.error('Erreur lors du chargement des templates');
        }
      });
  }

  selectTemplate(template: ContractTemplate): void {
    this.selectedTemplate = template;
    this.loadPreview();
  }

  loadPreview(): void {
    if (!this.selectedTemplate || !this.rentalId) return;

    this.loadingPreview = true;
    this.templateService.previewTemplate(this.selectedTemplate.id, this.rentalId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingPreview = false)
      )
      .subscribe({
        next: (preview) => {
          this.preview = preview;
        },
        error: (err) => {
          console.error('Error loading preview:', err);
          this.notificationService.error('Erreur lors de la prévisualisation');
          this.preview = null;
        }
      });
  }

  useDefaultTemplate(): void {
    this.selectedTemplate = null;
    this.preview = null;
  }

  confirm(): void {
    this.onConfirm.emit(this.selectedTemplate?.id || null);
  }

  cancel(): void {
    this.onCancel.emit();
  }

  isTemplateSelected(template: ContractTemplate): boolean {
    return this.selectedTemplate?.id === template.id;
  }

  isDefaultSelected(): boolean {
    return this.selectedTemplate === null;
  }
}
