import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { SignaturePadComponent } from './components/signature-pad/signature-pad.component';
import { DocumentUploadComponent } from './components/document-upload/document-upload.component';
import { DocumentListComponent } from './components/document-list/document-list.component';
import { PropertyPhotoUploadComponent } from './components/property-photo-upload/property-photo-upload.component';
import { PropertyPhotoGalleryComponent } from './components/property-photo-gallery/property-photo-gallery.component';
import { ListingPhotoUploadComponent } from './components/listing-photo-upload/listing-photo-upload.component';
import { ToastComponent } from './components/toast/toast.component';
import { NotificationComponent } from './components/notification/notification.component';
import { TemplateSelectorDialogComponent } from './components/template-selector-dialog/template-selector-dialog.component';

import { WidgetConfiguratorComponent } from './components/widget-configurator/widget-configurator.component';

// New shared components
import { PageHeaderComponent } from './components/page-header/page-header.component';
import { EmptyStateComponent } from './components/empty-state/empty-state.component';
import { LoadingSkeletonComponent } from './components/loading-skeleton/loading-skeleton.component';
import { StatusBadgeComponent } from './components/status-badge/status-badge.component';
import { StatsCardComponent } from './components/stats-card/stats-card.component';
import { DataTableComponent } from './components/data-table/data-table.component';
import { ConfirmDialogComponent } from './components/confirm-dialog/confirm-dialog.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { TopbarComponent } from './components/topbar/topbar.component';
import { QuickFiltersComponent } from './components/quick-filters/quick-filters.component';
import { NpsPromptComponent } from './components/nps-prompt/nps-prompt.component';
import { UpgradeModalComponent } from './components/upgrade-modal/upgrade-modal.component';
import { OnboardingChecklistComponent } from './components/onboarding-checklist/onboarding-checklist.component';

@NgModule({
  declarations: [
    SignaturePadComponent,
    DocumentUploadComponent,
    DocumentListComponent,
    PropertyPhotoUploadComponent,
    PropertyPhotoGalleryComponent,
    ListingPhotoUploadComponent,
    ToastComponent,
    NotificationComponent,
    TemplateSelectorDialogComponent,
    // New components
    PageHeaderComponent,
    EmptyStateComponent,
    LoadingSkeletonComponent,
    StatusBadgeComponent,
    StatsCardComponent,
    DataTableComponent,
    ConfirmDialogComponent,
    SidebarComponent,
    TopbarComponent,
    QuickFiltersComponent,
    NpsPromptComponent,
    WidgetConfiguratorComponent,
    UpgradeModalComponent,
    OnboardingChecklistComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    DragDropModule
  ],
  exports: [
    SignaturePadComponent,
    DocumentUploadComponent,
    DocumentListComponent,
    PropertyPhotoUploadComponent,
    PropertyPhotoGalleryComponent,
    ListingPhotoUploadComponent,
    ToastComponent,
    NotificationComponent,
    TemplateSelectorDialogComponent,
    DragDropModule,
    // New components
    PageHeaderComponent,
    EmptyStateComponent,
    LoadingSkeletonComponent,
    StatusBadgeComponent,
    StatsCardComponent,
    DataTableComponent,
    ConfirmDialogComponent,
    SidebarComponent,
    TopbarComponent,
    QuickFiltersComponent,
    NpsPromptComponent,
    WidgetConfiguratorComponent,
    UpgradeModalComponent,
    OnboardingChecklistComponent
  ]
})
export class SharedModule { }
