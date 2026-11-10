export interface NavItem {
  label: string;
  route: string;
  icon: string;
  section: 'core' | 'clinical' | 'operations' | 'administration';
  phaseTag?: string;
}

export interface BreadcrumbItem {
  label: string;
  route: string;
}
